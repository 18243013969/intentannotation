package com.luzhongwen.compliter;

import com.google.auto.service.AutoService;
import com.luzhongwen.intentannotation.Optional;
import com.luzhongwen.intentannotation.Required;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import jdk.nashorn.internal.runtime.regexp.joni.constants.NodeType;


@AutoService(Processor.class)
public class IntentProcessor extends AbstractProcessor {


    /**
     * 生成代码用的
     */
    private Filer mFileUtils;


    private Elements mElementUtils;

    private Messager messager;

    private Types mTypesUtils;

    Set<String> annotationNameSet = new LinkedHashSet<>(2);
    Set<Class<? extends Annotation>> annotationClassSet = new LinkedHashSet<>(2);

    {
        annotationNameSet.add(Required.class.getCanonicalName());
        annotationNameSet.add(Optional.class.getCanonicalName());

        annotationClassSet.add(Required.class);
        annotationClassSet.add(Optional.class);
    }

    private static final Diagnostic.Kind mKind = Diagnostic.Kind.NOTE;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFileUtils = processingEnvironment.getFiler();
        mElementUtils = processingEnvironment.getElementUtils();
        mTypesUtils = processingEnvironment.getTypeUtils();
        messager = processingEnvironment.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {//获取注解类型
        return annotationNameSet;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {//获取java版本
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        parseElements(roundEnvironment);
        return false;
    }

    private void parseElements(RoundEnvironment roundEnvironment) {
        Map<String, ProxyInfo> map = new HashMap<>();
        for (Class<? extends Annotation> annotation : annotationClassSet) {
            Set<? extends Element> elementSet = roundEnvironment.getElementsAnnotatedWith(annotation);
            if (elementSet != null) {
                for (Element e : elementSet) {
                    if (validElement(e)) {
                        addElement(map, (VariableElement) e);
                    } else {
                        printMsg("element不合法1");
                    }
                }
            }
        }

        if (map.size() > 0) {
            createFile(map);
        }
    }

    private boolean validElement(Element element) {
        if (element != null && element instanceof VariableElement) {
            TypeElement typeElement = (TypeElement) element.getEnclosingElement();
            if (typeElement.getKind() != ElementKind.CLASS) {
                return false;
            }
            boolean hasModifierError = false;
            for (Modifier modifier : element.getModifiers()) {
                if (modifier == Modifier.PRIVATE) {
                    hasModifierError = true;
                }
                if (modifier == Modifier.STATIC
                        || modifier == Modifier.FINAL) {
                    return false;
                }
            }
            return hasModifierError;
        }
        return false;
    }


    private void addElement(Map<String, ProxyInfo> map, VariableElement element) {
        TypeElement typeElement = (TypeElement) element.getEnclosingElement();
        final String className = typeElement.getQualifiedName().toString();
        //包名
        String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        ProxyInfo info;
        info = map.get(className);
        if (info == null) {
            info = ProxyInfo.newProxyInfo();
            info.setClassName(typeElement.getSimpleName().toString())
                    .setPk(packageName)
                    .addVariableElement(element);
            info.setTypesUtils(mTypesUtils);
            map.put(typeElement.getQualifiedName().toString(), info);
        } else {
            info.addVariableElement(element);
        }
        printElement(element);

        List<? extends AnnotationMirror> annotationMirrors = element
                .getAnnotationMirrors();
        AnnotationMirror annotationMirror = annotationMirrors.get(0);

        printMsg("annotationMirror " + annotationMirror.getAnnotationType() + " " + annotationMirrors.size());

    }

    private void createFile(Map<String, ProxyInfo> map) {
        for (ProxyInfo p : map.values()) {
            try {
                p.createJavaFile().writeTo(mFileUtils);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void printTypeElements(Set<? extends TypeElement> set) {
        if (set == null) {
            messager.printMessage(mKind, "Set<? extends TypeElement> set = null");
            return;
        }
        for (TypeElement e : set) {
            printTypeElement(e);
        }
    }

    private void printTypeElement(TypeElement element) {
        printMsg(getTypeElement(element));
    }

    private void printElement(Element element) {
        printMsg(getElement(element));
    }

    private String getTypeElement(TypeElement element) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("TypeElement [ \n");
        List<? extends Element> list = element.getEnclosedElements();
        if (list == null) {
            stringBuilder.append(" EnclosedElements = null");
        } else {
            for (Element e : list) {
                stringBuilder.append("\n");
                stringBuilder.append(getElement(e));
            }
        }
        stringBuilder.append("QualifiedName = " + element.getQualifiedName().toString() + "\n");
        stringBuilder.append("SimpleName = " + element.getSimpleName().toString() + "\n");
        stringBuilder.append("superClass = " + element.getSuperclass().toString());
        return stringBuilder.toString();

    }

    private String getElement(Element element) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("element = " + element.getClass() + " [ \n");
        TypeMirror type = element.asType();
        if(type instanceof ReferenceType) {
            stringBuilder.append("TypeMirror = ReferenceType"  + "\n");

        }
        if(type instanceof DeclaredType) {
            stringBuilder.append("TypeMirror = DeclaredType"  + ((DeclaredType)type).toString() + "\n");
        }
        if(type instanceof TypeVariable){
            stringBuilder.append("TypeMirror = TypeVariable"  + "\n");
        }
        if(type instanceof ErrorType){
            stringBuilder.append("TypeMirror = ErrorType"  + "\n");
        }
        if(type instanceof PrimitiveType){
            stringBuilder.append("TypeMirror = PrimitiveType"  + "\n");
        }
        if(type instanceof ExecutableType){
            stringBuilder.append("TypeMirror = ExecutableType"  + "\n");
        }
        stringBuilder.append("TypeMirror = " + type.toString() + " " + type.getKind().toString() + "\n");
        stringBuilder.append("Kind = " + element.getKind().toString() + "\n");
        stringBuilder.append("Modifiers = ");
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers == null) {
            stringBuilder.append("null\n");
        } else {
            for (Modifier m : modifiers) {
                stringBuilder.append(" " + m.toString());
            }
            stringBuilder.append("\n");
        }
        stringBuilder.append("SimpleName = " + element.getSimpleName() + "\n");
        stringBuilder.append("EnclosedElements = ");
        List<? extends Element> elements = element.getEnclosedElements();
        if (element == null) {
            stringBuilder.append("null");
        } else {
            for (Element e : elements) {
                stringBuilder.append("\n");
                if (e instanceof TypeElement) {
                    stringBuilder.append(getTypeElement((TypeElement) e));
                } else {
                    stringBuilder.append(getElement(e));
                }
            }
        }
        stringBuilder.append("\n");
        stringBuilder.append("AnnotationMirrors = ");
        List<? extends AnnotationMirror> mirrors = element.getAnnotationMirrors();
        if (mirrors == null) {
            stringBuilder.append("null");
        } else {
            for (AnnotationMirror a : mirrors) {
                stringBuilder.append("\n");
                stringBuilder.append("AnnotationMirror = " + a.toString());
            }
        }
        return stringBuilder.toString();

    }


    private void printRoundEnvironment(RoundEnvironment roundEnvironment) {
        Set<? extends Element> set = roundEnvironment.getElementsAnnotatedWith(Required.class);
        Iterator<? extends Element> iterator = set.iterator();
        Element element;
        while (iterator.hasNext()) {
            element = iterator.next();
            printElement(element);
        }
    }

    private void printMsg(String msg) {
        messager.printMessage(mKind, msg);
    }
}
