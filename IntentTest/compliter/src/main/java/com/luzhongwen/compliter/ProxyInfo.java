package com.luzhongwen.compliter;

import com.luzhongwen.intentannotation.Required;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public class ProxyInfo {
    private String pk;
    private String className;
    private String classNameExpand;
    private final List<VariableElement> list = new ArrayList<>();
    public final static String CLASS_SUFFIX = "Builder";

    private Types typesUtils;


    public static ProxyInfo newProxyInfo() {
        return new ProxyInfo();
    }

    public ProxyInfo setPk(String pk) {
        if(pk == null) {
            throw new NullPointerException("className = null");
        }
        this.pk = pk;
        return this;
    }

    public ProxyInfo setClassName(String className) {
        if(className == null) {
            throw new NullPointerException("className = null");
        }
        this.className = className;
        this.classNameExpand = this.className.concat(CLASS_SUFFIX);
        return this;
    }

    public ProxyInfo setTypesUtils(Types typesUtils) {
        this.typesUtils = typesUtils;
        return this;
    }

    public ProxyInfo addVariableElement(VariableElement variableElement) {
        if (!list.contains(variableElement)) {
            list.add(variableElement);
        } else {
            System.out.print("已经包含");
        }

        return this;
    }

    /**
     * 生成读写的文件
     **/
    public JavaFile createJavaFile() {
        TypeSpec.Builder builder = createClassBuilder();
        for (VariableElement v : list
        ) {
            builder.addField(createFieldSpecByVariableElement(v));
            builder.addMethod(createMethodByVariableElement(v));
        }
        builder.addMethod(createIntent());
        return getJavaFile(builder.build());
    }
    /**
     * 生成
     * **/
    private JavaFile getJavaFile(TypeSpec typeSpec) {
        return JavaFile.builder(pk, typeSpec)
                .addFileComment("Generated code from intentcompiler. Do not modify!")
                .build();
    }

    /**
     * 创建类
     * **/
    private TypeSpec.Builder createClassBuilder() {
        return TypeSpec.classBuilder(classNameExpand)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    }

    /**
     * 通过VariableElement创建全局变量
     **/
    private FieldSpec createFieldSpecByVariableElement(VariableElement element) {
        return FieldSpec
                .builder(getTypeName(element)
                        , getVariableElementSimpleName(element)
                        , Modifier.PRIVATE).build();

    }

    /**
     * 创建intent
     * **/
    private MethodSpec createIntent() {
        final ClassName intentClassName = getClassName("android.content"
                , "Intent");
        final String contextParameter = "context";
        MethodSpec.Builder builder = MethodSpec.methodBuilder("start" + className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(void.class)
                .addParameter(getClassName("android.content",
                        "Context"), contextParameter)
                .addStatement("$T intent = new $T($N,$T.class)", intentClassName,
                        intentClassName, contextParameter, getClassName(pk, className));
        for (VariableElement element : list) {
            Required required = element.getAnnotation(Required.class);
            final String simpleName = getVariableElementSimpleName(element);
            if (required != null) {
                if(!getTypeName(element).isPrimitive()) {
                    builder.beginControlFlow("if(this.$N == null)", simpleName);
                    builder.addStatement("throw new NullPointerException($S)"
                    ,"this. " + simpleName+ " 不能为null");
                    builder.endControlFlow();
                }
            }
            TypeMirror type = element.asType();
             if(type instanceof DeclaredType) {
                 final String typeString = type.toString();
                if(typeString.equals("java.util.ArrayList<java.lang.String>")) {
                    putStringArrayListExtra(builder,simpleName);
                } else if(typeString.equals("java.util.ArrayList<java.lang.Integer>")){
                   putIntegerArrayListExtra(builder,simpleName);
                }else if(typeString.equals("java.util.ArrayList<? extends android.os.Parcelable>")){
                    putParcelableArrayListExtra(builder,simpleName);
                } else if(typeString.equals("java.util.ArrayList<java.lang.CharSequence>")) {
                    putCharSequenceArrayListExtra(builder,simpleName);
                } else {
                    putExtra(builder,simpleName);
                }
             } else {
                 putExtra(builder,simpleName);
             }
        }
        builder.addCode(contextParameter + ".startActivity(intent);\n");
        return builder.build();
    }

    private void putExtra(MethodSpec.Builder builder, String simpleName){
        setExtra(builder,simpleName,"putExtra");
    }

    private void setExtra(MethodSpec.Builder builder, String simpleName, String methodName){
        builder.addStatement("intent.$N($S" + ",this." + simpleName + ")",methodName,simpleName);

    }

    private void putCharSequenceArrayListExtra(MethodSpec.Builder builder, String simpleName) {
        setExtra(builder,simpleName,"putCharSequenceArrayListExtra");

    }

    private void putStringArrayListExtra(MethodSpec.Builder builder, String simpleName){
        setExtra(builder,simpleName,"putStringArrayListExtra");
    }

    private void putIntegerArrayListExtra(MethodSpec.Builder builder, String simpleName){
        setExtra(builder,simpleName,"putIntegerArrayListExtra");
    }

    private void putParcelableArrayListExtra(MethodSpec.Builder builder, String simpleName){
        setExtra(builder,simpleName,"putParcelableArrayListExtra");

    }

    private MethodSpec createMethodByVariableElement(VariableElement element) {
        final String simpleName = getVariableElementSimpleName(element);
        return MethodSpec.methodBuilder("set" + getFirstUpperCase(simpleName))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(getClassName(pk, classNameExpand))
                .addParameter(getTypeName(element),
                        simpleName)
                .addStatement("this.$N = $N", simpleName, simpleName)
                .addStatement("return this")
                .build();
    }

    private TypeName getTypeName(VariableElement element) {
        return TypeName.get(element.asType());
    }

    private String getVariableElementSimpleName(VariableElement element) {
        return element.getSimpleName().toString();
    }

    private ClassName getClassName(String packageName, String className) {
        return ClassName.get(packageName,
                className);
    }

    private String getFirstUpperCase(String str) {
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("str = null");
        }
        if (str.length() == 1) {
            return str.toUpperCase();
        } else {
            return str.substring(0, 1)
                    .toUpperCase()
                    .concat(str.substring(1));//concat 通过arraycopy实现，性能更好

        }
    }
}
