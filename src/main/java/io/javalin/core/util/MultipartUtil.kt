/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.UploadedFile
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.Part

object MultipartUtil {

    fun getUploadedFiles(servletRequest: HttpServletRequest, partName: String): List<UploadedFile> {
        servletRequest.setAttribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement(System.getProperty("java.io.tmpdir")))
        return servletRequest.parts.filter { isFile(it) && it.name == partName }.map { filePart ->
            val submittedFileName = getNameFromContentDisposition(filePart)
            UploadedFile(
                    contentType = filePart.contentType,
                    content = ByteArrayInputStream(filePart.inputStream.readBytes()),
                    name = submittedFileName,
                    extension = submittedFileName.replaceBeforeLast(".", "")
            )
        }
    }

    private fun getNameFromContentDisposition(part: Part): String{
        val cd = part.getHeader("Content-Disposition")?.toLowerCase() ?: return part.name
        if(cd.startsWith("form-data") || cd.startsWith("attachment")){
            val p = cd.split(";").firstOrNull { it.contains("filename") } ?: return part.name
            val fn = p.split("=")[1]
            val fnNoQuote = fn.substring(1, fn.length-1)
            return fnNoQuote.replace(Regex("[^0-9A-Za-z\\s\\.\\-]"),"_")

        }
        return part.name

    }

    fun getFieldMap(req: HttpServletRequest): Map<String, List<String>> {
        req.setAttribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement(System.getProperty("java.io.tmpdir")))
        return req.parts.associate { part -> part.name to getPartValue(req, part.name) }
    }

    private fun getPartValue(req: HttpServletRequest, partName: String): List<String> {
        return req.parts.filter { isField(it) && it.name == partName }.map { filePart ->
            filePart.inputStream.readBytes().toString(Charset.forName("UTF-8"))
        }.toList()
    }

    private fun isField(filePart: Part) = filePart.name == null // this is what Apache FileUpload does ...
    private fun isFile(filePart: Part) = !isField(filePart)
}
