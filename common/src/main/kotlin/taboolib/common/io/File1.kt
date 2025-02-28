package taboolib.common.io

import java.io.File

/**
 * 声明一个文件
 *
 * @param file 所属文件
 * @param path 文件路径
 * @param create 若文件不存在是否新建（默认为是）
 * @param folder 该文件是否为文件夹（默认为否）
 * @return 该文件自身
 */
fun newFile(file: File, path: String, create: Boolean = true, folder: Boolean = false): File {
    return newFile(File(file, path), create, folder)
}

/**
 * 声明一个文件
 *
 * @param path 文件路径
 * @param create 若文件不存在是否新建（默认为是）
 * @param folder 该文件是否为文件夹（默认为否）
 * @return 该文件自身
 */
fun newFile(path: String, create: Boolean = true, folder: Boolean = false): File {
    return newFile(File(path), create, folder)
}

/**
 * 声明一个文件
 *
 * @param file 文件
 * @param create 若文件不存在是否新建（默认为是）
 * @param folder 该文件是否为文件夹（默认为否）
 * @return 该文件自身
 */
fun newFile(file: File, create: Boolean = true, folder: Boolean = false): File {
    if (!file.parentFile.exists()) {
        file.parentFile.mkdirs()
    }
    if (!file.exists() && create) {
        if (folder) {
            file.mkdirs()
        } else {
            file.createNewFile()
        }
    }
    return file
}