package com.example.groupproject.service;

import com.example.groupproject.dto.ApplicationDetailDTO;
import com.example.groupproject.entity.Application;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;

@Service
public class ApplicationDetailService {

    private final ApplicationRepository applicationRepository;

    public ApplicationDetailService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public ApplicationDetailDTO getApplicationDetail(Long applicationId, User currentUser) {
        // 1. Sử dụng phương thức findByIdWithDetails đã thêm JOIN FETCH
        // Điều này đảm bảo dữ liệu Candidate và Job được tải ngay, tránh lỗi Lazy loading
        Application app = applicationRepository.findByIdWithDetails(applicationId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn ứng tuyển với ID: " + applicationId));

        // 2. Kiểm tra quyền truy cập ghi chú nội bộ
        boolean canViewNotes = (currentUser.getRole() == UserRole.ADMIN ||
                currentUser.getRole() == UserRole.HR_MANAGER);

        // 3. Chuyển đổi từ Entity sang DTO
        // Lưu ý: Đảm bảo class User của bạn có các phương thức getFullName() và getEmail()
        ApplicationDetailDTO dto = new ApplicationDetailDTO(
                app.getId().longValue(),
                app.getCandidate().getFullName(),
                app.getCandidate().getEmail(),
                Date.from(app.getSubmittedAt()),
                app.getStatus().name(),
                canViewNotes
        );

        // 4. Xử lý logic ghi chú (Gán dữ liệu vào DTO)
        if (canViewNotes) {
            // Sau này bạn có thể nạp dữ liệu thật từ bảng notes vào đây
            dto.setInternalNotes(Collections.emptyList());
        }

        return dto;
    }
    
    public Long findAppIdByUserId(Long userId) {
        // Chúng ta tìm ứng viên (Candidate) có user_id tương ứng
        // Sau đó lấy Application đầu tiên mà ứng viên đó đã nộp
        return applicationRepository.findByCandidateId(userId.intValue())
                .map(app -> app.getId().longValue())
                .orElse(null);
    }
}