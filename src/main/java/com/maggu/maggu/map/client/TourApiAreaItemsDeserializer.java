package com.maggu.maggu.map.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.List;

/*
 * TourAPI 응답의 items는 결과 0건이면 빈 문자열("")로,
 * 결과 1건이면 item이 배열이 아닌 단일 객체로 내려온다(공공데이터 API 특유의 XML->JSON 변환 트릭)
 * 세 가지 형태(""/단일객체/배열) 전부 List<AreaBasedItem>으로 정규화한다.
 *
 * 추후 raw response가 더 늘어난다면 제너릭하게 고치는 것을 고려!
 */

class TourApiAreaItemsDeserializer extends StdDeserializer<List<AreaBasedItem>> {

    TourApiAreaItemsDeserializer() {
        super(List.class);
    }

    @Override
    public List<AreaBasedItem> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode itemsNode = mapper.readTree(parser);

        if (!itemsNode.isObject()) {
            return List.of();
        }

        JsonNode item = itemsNode.get("item");
        if (item == null || item.isNull()) {
            return List.of();
        }
        if (item.isArray()) {
            return mapper.convertValue(item,
                    mapper.getTypeFactory().constructCollectionType(List.class, AreaBasedItem.class));
        }
        return List.of(mapper.treeToValue(item, AreaBasedItem.class));
    }
}
