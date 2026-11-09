package com.mycompany.myapp.service.listingimport;

import java.time.Instant;
import java.util.Iterator;

/** Nguồn dữ liệu nhập, đọc lần lượt (file lớn / API nhiều trang không nạp hết vào bộ nhớ). */
public interface RecordSource extends Iterator<RecordSource.SourceRow>, AutoCloseable {
    /**
     * @param rowNo số dòng để admin tra lại: số dòng trong Excel/CSV (dòng tiêu đề là 1), thứ tự bản ghi trong API (từ 1)
     * @param listingId doanh nghiệp nguồn đã khớp sẵn; null thì để quy trình nhập tự kiểm tra trùng
     * @param slug slug nguồn cấp, dùng cho doanh nghiệp tạo mới nếu chưa ai dùng; null thì sinh từ tên
     * @param refresh nguồn mới hơn dữ liệu đang có: ngoài điền ô trống, còn đè thông tin doanh nghiệp
     * @param sourceUpdatedAt ngày cập nhật của bản nguồn; null nếu nguồn không có
     */
    record SourceRow(int rowNo, ImportRecord record, Long listingId, String slug, boolean refresh, Instant sourceUpdatedAt) {
        public SourceRow(int rowNo, ImportRecord record) {
            this(rowNo, record, null, null, false, null);
        }
    }

    /** Gọi khi đã xử lý hết mọi dòng, không lỗi. */
    default void onSuccess() {}

    @Override
    void close();
}
