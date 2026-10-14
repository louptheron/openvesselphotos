package org.openvesselphotos.domain.images

interface ImageDownloader {
    /** Downloads an image from a host photos are imported from (Wikimedia Commons, Flickr). */
    fun download(url: String): ByteArray
}
