package com.maggu.maggu.map.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.List;

/*
 * TourAPI 응답의 items는 결과 0건이면 빈 문자열("")로,
 * 결과 1건이면 item이 배열이 아닌 단일 객체로 내려온다(공공데이터 API 특유의 XML->JSON 변환 트릭)
 * 세 가지 형태(""/단일객체/배열) 전부 List<T>로 정규화한다.
 *
 * T는 런타임에 소거되므로, 실제 아이템 타입은 createContextual()에서
 * 이 디시리얼라이저가 붙은 필드(List<T> items)의 JavaType으로부터 얻어 contentType에 저장해둔다.
 */
class TourApiItemsDeserializer extends StdDeserializer<List<Object>> implements ContextualDeserializer {

    private final JavaType contentType;

    TourApiItemsDeserializer() {
        this(null);
    }

    private TourApiItemsDeserializer(JavaType contentType) {
        super(List.class);
        this.contentType = contentType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType wrapperType = property.getType(); // List<T>가 이미 구체화된 JavaType
        JavaType itemType = wrapperType.getContentType();
        return new TourApiItemsDeserializer(itemType);
    }

    @Override
    public List<Object> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
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
            JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, contentType);
            return mapper.convertValue(item, listType);
        }

        /*
         * List.of(mapper.convertValue(item, contentType))로 한 줄로 합치지 말 것:
         * convertValue의 반환 타입 T가 인자로부터 아무 힌트를 못 받는 상태라 List.of의 오버로드
         * 해석이 꼬여서(가변인자로 오인) ClassCastException이 남.
         * 변수로 먼저 타입을 확정해야 함.
         * */
        Object singleItem = mapper.convertValue(item, contentType);
        return List.of(singleItem);
    }
}
