package vn.com.detai.todolist.adapter

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper

import vn.com.detai.todolist.activity.MainActivity

/**
 * Enables basic drag & drop and swipe-to-dismiss. Drag events are automatically started by an item long-press.
 */
open class ListItemTouchHelper protected constructor(private val mAdapter: RecyclerViewAdapter, private val mRecyclerView: RecyclerView) : ItemTouchHelper.Callback() {

    /**
     * Cho phép bạn đặt cờ chuyển động cho từng mục trong RecyclerView
     * Nên trả về một cờ tổng hợp xác định các hướng di chuyển được bật ở mỗi trạng thái (không hoạt động, vuốt, kéo).
     */
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        if (MainActivity.mSearchViewIsOpen) return 0
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN // chuyển động lên xuống
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END // chuyển động qua trái hay phải
        return ItemTouchHelper.Callback.makeMovementFlags(dragFlags, swipeFlags)
    }

    /**
     * Được gọi khi ItemTouchHelper muốn di chuyển ghi chú được kéo từ vị trí cũ sang vị trí mới.
     */
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        mAdapter.moveTask(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    /**
     * Được gọi khi người dùng vuốt ViewHolder.
     */
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) =
        mAdapter.removeTask(viewHolder.adapterPosition, mRecyclerView)

    /**
     * Enable the ability to move items.
     * Returns whether ItemTouchHelper should start a drag and drop operation if an item is long pressed.
     */
    override fun isLongPressDragEnabled() = true

    /**
     * Enable the ability to swipe items.
     * Returns whether ItemTouchHelper should start a swipe operation if a pointer is swiped over the View.
     */
    override fun isItemViewSwipeEnabled() = true
}
