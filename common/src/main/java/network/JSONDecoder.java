package network;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JSONDecoder extends MessageToMessageDecoder<byte[]> {
    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void decode(ChannelHandlerContext ctx, byte[] msg, List<Object> out) throws Exception {
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        out.add(objectMapper.readValue(msg, Message.class));
    }

    private ArrayList<?> deserializeArrayList(byte[] msg, Class<?> objectClass) throws IOException {
        CollectionType typeReference = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, objectClass);
        return objectMapper.readValue(msg, typeReference);
    }

}
