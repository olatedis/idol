package com.bit.docker.openapiservice.perse;

import com.bit.docker.openapiservice.dto.ConcertResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class KopisXmlParser {

    public static List<ConcertResponse> parse(String xml) {
        List<ConcertResponse> result = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList nodes = doc.getElementsByTagName("db");

            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);

                result.add(new ConcertResponse(
                        getText(e, "mt20id"),
                        getText(e, "prfnm"),
                        getText(e, "prfpdfrom"),
                        getText(e, "prfpdto"),
                        getText(e, "fcltynm"),
                        getText(e, "poster")
                ));
            }

        } catch (Exception e) {
            throw new RuntimeException("KOPIS XML 파싱 실패", e);
        }

        System.out.println(result);
        return result;
    }

    private static String getText(Element e, String tag) {
        NodeList list = e.getElementsByTagName(tag);
        return list.getLength() > 0 ? list.item(0).getTextContent() : "";
    }
}

