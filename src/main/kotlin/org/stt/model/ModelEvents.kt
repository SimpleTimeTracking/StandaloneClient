package org.stt.model


interface ItemModified

class ItemDeleted(val deletedItem: TimeTrackingItem) : ItemModified
class ItemInserted(val newItem: TimeTrackingItem) : ItemModified
class ItemReplaced(val beforeUpdate: TimeTrackingItem, val afterUpdate: TimeTrackingItem) : ItemModified
