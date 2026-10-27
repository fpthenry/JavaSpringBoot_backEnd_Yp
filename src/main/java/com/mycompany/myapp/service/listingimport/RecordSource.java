package com.mycompany.myapp.service.listingimport;

import java.util.Iterator;

/** Nguồn dữ liệu nhập, đọc lần lượt (file lớn / API nhiều trang không nạp hết vào bộ nhớ). */
public interface RecordSource extends Iterator<RecordSource.SourceRow>, AutoCloseable {
    /**
     * @param rowNo số dòng để admin tra lại: số dòng trong Excel/CSV (dòng tiêu đề là 1), thứ tự bản ghi trong API (từ 1)
     */
    record SourceRow(int rowNo, ImportRecord record) {}

    @Override
    void close();
}
