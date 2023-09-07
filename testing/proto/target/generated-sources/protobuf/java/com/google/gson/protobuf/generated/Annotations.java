// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: annotations.proto

package com.google.gson.protobuf.generated;

public final class Annotations {
  private Annotations() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
    registry.add(com.google.gson.protobuf.generated.Annotations.serializedName);
    registry.add(com.google.gson.protobuf.generated.Annotations.serializedValue);
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public static final int SERIALIZED_NAME_FIELD_NUMBER = 92066888;
  /**
   * <pre>
   * Indicates a field name that overrides the default for serialization
   * </pre>
   *
   * <code>extend .google.protobuf.FieldOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.FieldOptions,
      java.lang.String> serializedName = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.String.class,
        null);
  public static final int SERIALIZED_VALUE_FIELD_NUMBER = 92066888;
  /**
   * <pre>
   * Indicates a field value that overrides the default for serialization
   * </pre>
   *
   * <code>extend .google.protobuf.EnumValueOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.EnumValueOptions,
      java.lang.String> serializedValue = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.String.class,
        null);

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\021annotations.proto\022\036google.gson.protobu" +
      "f.generated\032 google/protobuf/descriptor." +
      "proto:9\n\017serialized_name\022\035.google.protob" +
      "uf.FieldOptions\030\310\250\363+ \001(\t:>\n\020serialized_v" +
      "alue\022!.google.protobuf.EnumValueOptions\030" +
      "\310\250\363+ \001(\tB$\n\"com.google.gson.protobuf.gen" +
      "erated"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.DescriptorProtos.getDescriptor(),
        });
    serializedName.internalInit(descriptor.getExtensions().get(0));
    serializedValue.internalInit(descriptor.getExtensions().get(1));
    com.google.protobuf.DescriptorProtos.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
