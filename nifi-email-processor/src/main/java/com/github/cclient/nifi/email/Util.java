package com.github.cclient.nifi.email;

import org.apache.nifi.flowfile.FlowFile;

import java.util.*;
import java.util.stream.Collectors;

public class Util {
    public static String buildEmailHtmlBodyByAttr(List<FlowFile> flowFileList) {
        List<Map<String, String>> attrList = flowFileList.stream().map(flowFile -> flowFile.getAttributes()).collect(Collectors.toList());
        List<String> allKeys = attrList.stream().map(attr -> attr.keySet()).flatMap(keys -> keys.stream()).distinct().collect(Collectors.toList());
        Collections.sort(allKeys);
        StringBuilder sb = new StringBuilder();
        sb.append("<table border=\"1\" cellspacing=\"0\" >");
        sb.append("<thead align=\"center\" valign=\"middle\">");
        sb.append("<tr>");
        allKeys.forEach(key -> sb.append("<th>" + key + "</th>"));
        sb.append("<th>entryDate</th>");
        sb.append("<th>size</th>");
        sb.append("</tr>");
        sb.append("</thead>");
        sb.append("<tbody>");
        flowFileList.stream().forEach(flowFile -> {
            Map<String, String> kvs = flowFile.getAttributes();
            sb.append("<tr>");
            allKeys.forEach(key -> sb.append("<td>" + kvs.getOrDefault(key, "") + "</td>"));
            sb.append("<td>" + new Date(flowFile.getEntryDate()) + "</td>");
            sb.append("<td>" + flowFile.getSize() + "</td>");
            sb.append("</tr>");
        });
        sb.append("</tbody>");
        sb.append("</table>");
        return sb.toString();
    }
}
