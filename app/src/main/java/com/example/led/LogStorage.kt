package com.example.led

object LogStorage {
    val logs: MutableList<String> = object : AbstractMutableList<String>() {
        private val internalList: MutableList<String> = mutableListOf()
        override val size: Int
            get() = internalList.size

        override fun get(index: Int): String = internalList[index]

        override fun set(index: Int, element: String): String = internalList.set(index, element)

        override fun add(index: Int, element: String) {
            internalList.add(index, element)
        }

        override fun add(element: String): Boolean {
            val newIndex = size + 1 // Incrementing the index by 1
            //return internalList.add("[$newIndex] $element")
            return internalList.add(element)
        }

        override fun removeAt(index: Int): String = internalList.removeAt(index)
    }
}