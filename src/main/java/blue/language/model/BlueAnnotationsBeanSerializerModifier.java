package blue.language.model;

import blue.language.utils.BlueIdResolver;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;

public class BlueAnnotationsBeanSerializerModifier extends BeanSerializerModifier {
    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        if (BlueIdResolver.resolveBlueId(beanDesc.getBeanClass()) != null && serializer instanceof BeanSerializerBase)
            return new BlueAnnotationsSerializer((BeanSerializerBase) serializer);
        return serializer;
    }
}