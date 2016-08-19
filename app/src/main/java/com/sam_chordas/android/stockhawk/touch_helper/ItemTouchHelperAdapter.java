package com.sam_chordas.android.stockhawk.touch_helper;

/**
 * Created by sam_chordas on 10/6/15.
 * credit to Paul Burke (ipaulpro)
 * Interface to enable swipe to delete
 */
//The next two, onMove() and onSwiped() are needed to notify anything in charge
// of updating the underlying data.
// So first we’ll create an interface that allows us to pass these event callbacks back up the chain.
public interface ItemTouchHelperAdapter {

  void onItemDismiss(int position);
}
