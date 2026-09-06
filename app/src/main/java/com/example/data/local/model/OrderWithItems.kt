package com.example.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entities.CarpetItemEntity
import com.example.data.local.entities.OrderEntity

data class OrderWithItems(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<CarpetItemEntity> = emptyList()
)
