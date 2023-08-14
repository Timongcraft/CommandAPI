package dev.jorel.commandapi.nms;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import dev.jorel.commandapi.SafeStaticOneParameterMethodHandle;
import dev.jorel.commandapi.SafeVarHandle;
import dev.jorel.commandapi.arguments.ExceptionHandlingArgumentType;
import dev.jorel.commandapi.preprocessor.Differs;
import net.minecraft.server.v1_16_R2.ArgumentRegistry;
import net.minecraft.server.v1_16_R2.ArgumentSerializer;
import net.minecraft.server.v1_16_R2.MinecraftKey;
import net.minecraft.server.v1_16_R2.PacketDataSerializer;

@Differs(from = {"1.15", "1.16.1"}, by = "Renamed ArgumentRegistry#a(ArgumentType) to ArgumentRegistry#b(ArgumentType)")
public class ExceptionHandlingArgumentSerializer_1_16_R2<T, EI>
	extends ExceptionHandlingArgumentSerializer_Common<T, EI, PacketDataSerializer>
	implements ArgumentSerializer<ExceptionHandlingArgumentType<T, EI>> {
    // All the ? here should actually be ArgumentRegistry.a, but that is a private inner class. That makes everything really annoying.
    // TODO: We want to check this reflection, but we can't give ArgumentRegistry.a to the @RequireField annotation
    //  Hopefully something works out, but the preprocessor needs to be expanded first
    private static final SafeStaticOneParameterMethodHandle<?, ArgumentType<?>> getArgumentTypeInformation;
    private static final SafeVarHandle<?, MinecraftKey> serializationKey;
    private static final SafeVarHandle<?, ArgumentSerializer<ArgumentType<?>>> serializer;

    // Compute all var handles all in one go so we don't do this during main server runtime
    static {
        // We need a reference to the class object for ArgumentRegistry.a,
        // It can be found as the return type of ArgumentRegistry#get(ArgumentType)
        Class<?> entryClass = null;
        try {
            entryClass = ArgumentRegistry.class.getDeclaredMethod("b", ArgumentType.class).getReturnType();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }

        getArgumentTypeInformation = SafeStaticOneParameterMethodHandle.ofOrNull(ArgumentRegistry.class, "b", "b", entryClass, ArgumentType.class);
        serializationKey = SafeVarHandle.ofOrNull(entryClass, "c", "c", MinecraftKey.class);
        serializer = SafeVarHandle.ofOrNull(entryClass, "b", "b", ArgumentSerializer.class);
    }

    // Serializer_Common methods
    @Override
    protected Object getArgumentTypeInformation(ArgumentType<?> argumentType) {
        return getArgumentTypeInformation.invokeOrNull(argumentType);
    }

    @Override
    protected String getSerializationKey(Object info) {
        return serializationKey.getUnknownInstanceType(info).toString();
    }

    @Override
    protected void serializeBaseTypeToNetwork(ArgumentType<T> baseType, Object baseInfo, PacketDataSerializer packetWriter) {
        serializer.getUnknownInstanceType(baseInfo).a(baseType, packetWriter);
    }

    @Override
    protected void serializeBaseTypeToJson(ArgumentType<T> baseType, Object baseInfo, JsonObject properties) {
        serializer.getUnknownInstanceType(baseInfo).a(baseType, properties);
    }

    // ArgumentSerializer methods
    @Override
    // serializeToNetwork
    public void a(ExceptionHandlingArgumentType<T, EI> argument, PacketDataSerializer packetDataSerializer) {
        commonSerializeToNetwork(argument, packetDataSerializer);
    }

    @Override
    // serializeToJson
    public void a(ExceptionHandlingArgumentType<T, EI> argument, JsonObject properties) {
        commonSerializeToJson(argument, properties);
    }

    @Override
    // deserializeFromNetwork
    public ExceptionHandlingArgumentType<T, EI> b(PacketDataSerializer packetDataSerializer) {
        // Since this class overrides its ArgumentRegistry key with the baseType's,
        // this class's key should never show up in a packet and this method should never
        // be called to deserialize the ArgumentType info that wasn't put into the packet
        // anyway. Also, the server shouldn't ever deserialize a PacketPlay*Out*Commands
        // either. If this method ever gets called, either you or I are doing something very wrong!
        throw new IllegalStateException("This shouldn't happen! See dev.jorel.commandapi.nms.ExceptionHandlingArgumentSerializer_1_16_R2#b for more information");
        // Including a mini-stacktrace here in case this exception shows up
        // on a client-disconnected screen, which is not very helpful
    }
}