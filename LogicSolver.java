
package checkers.inference.solver.LogiqlDebugSolver;

import java.io.*;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import javax.swing.text.html.HTMLDocument.Iterator;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import checkers.inference.InferenceSolver;
import checkers.inference.model.CombVariableSlot;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.Constraint;
import checkers.inference.model.RefinementVariableSlot;
import checkers.inference.model.Slot;
import checkers.inference.model.VariableSlot;
import checkers.inference.util.InferenceUtil;
import ostrusted.quals.OsTrusted;
import ostrusted.quals.OsUntrusted;

import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * 
 *
 * @author Jianchu Li
 *
 */

public class LogicSolver implements InferenceSolver {

    Map<String, String> subtype = new HashMap<String, String>();
    Map<String, String> supertype = new HashMap<String, String>();
    Map<String, String> notComparable = new HashMap<String, String>();
    Set<? extends AnnotationMirror> allTypes;
    Set<? extends AnnotationMirror> top;
    Set<? extends AnnotationMirror> bottom;
    final String isAnnotated = "isAnnotated";
    final String mayBeAnnotated = "mayBeAnnotated";
    final String cannotBeAnnotated = "cannotBeAnnotated";

    @Override
    public Map<Integer, AnnotationMirror> solve(
            Map<String, String> configuration, Collection<Slot> slots,
            Collection<Constraint> constraints,
            QualifierHierarchy qualHierarchy,
            ProcessingEnvironment processingEnvironment) {
        top = qualHierarchy.getTopAnnotations();
        bottom = qualHierarchy.getBottomAnnotations();
        allTypes = qualHierarchy.getTypeQualifiers();
        Set<String> allTypesInString = new HashSet<String>();
        String encodingForEqualityConModifier = "";
        String basicEncoding = "";
        for (AnnotationMirror i : allTypes) {
            allTypesInString.add(i.toString().replaceAll("[.@]", "_"));
        }
        getSubSupertype(qualHierarchy);
        getNotComparable();
        basicEncoding = getBasicString(allTypesInString, basicEncoding);
        encodingForEqualityConModifier = getEncodingForEqualityConModifier(
                allTypesInString, encodingForEqualityConModifier);
        print();
        System.out.println(basicEncoding);
        System.out.println(encodingForEqualityConModifier);
        return null;

    }

    public void getSubSupertype(QualifierHierarchy hierarchy) {
        for (AnnotationMirror i : allTypes) {
            String subtypeFori = "";
            String supertypeFori = "";
            for (AnnotationMirror j : allTypes) {
                if (hierarchy.isSubtype(j, i)) {
                    subtypeFori = subtypeFori + " " + j.toString();
                }
                if (hierarchy.isSubtype(i, j)) {
                    supertypeFori = supertypeFori + " " + j.toString();
                }
            }
            supertype.put(i.toString(), supertypeFori);
            subtype.put(i.toString(), subtypeFori);
        }
    }

    public void getNotComparable() {
        for (AnnotationMirror i : allTypes) {
            String notComparableFori = "";
            for (AnnotationMirror j : allTypes) {
                if (!subtype.get(i.toString()).contains(j.toString())
                        && !subtype.get(j.toString()).contains(i.toString())) {
                    notComparableFori = notComparableFori + " " + j.toString();
                }
            }
            if (!notComparableFori.equals("")) {
                notComparable.put(i.toString(), notComparableFori);
            }

        }
    }

    public String getEncodingForEqualityConModifier(
            Set<String> allTypesInString, String encodingForEqualityConModifier) {
        for (String s : allTypesInString) {
            encodingForEqualityConModifier = encodingForEqualityConModifier
                    + isAnnotated
                    + s
                    + "[v2] = true <- equalityConstraintContainsModifier[v1,v2] = true, v1 = \""
                    + s + "\".\n";
        }
        return encodingForEqualityConModifier;
    }

    public String getBasicString(Set<String> allTypesInString,
            String basicEncoding) {
        basicEncoding = "variable(v), hasvariableName(v:i) -> int(i)."
                + "\nmodifier(m), hasmodifierName(m:i) -> string(i)."
                + "\nvariableOrder(v) -> int(v)."
                + "\nvariableOrder(v) <- variable(v)."
                + "\norderVariable[o] = v -> int(o), int(v)."
                + "\norderVariable[o] =v <- seq<<o=v>> variableOrder(v)."
                + "\norderedAnnotationOf[v] = a -> int(v), string(a)."
                + "\norderedAnnotationOf[v] = a <- AnnotationOf[v]=a, orderVariable[_]=v."
                + "\nAnnotationOf[v] = a -> variable(v), string(a)."
                + "\nadaptationConstraint[v1,v2,v3] = a -> variable(v1), variable(v2), variable(v3), boolean(a)."
                + "\nequalityConstraint[v1,v2] = e -> variable(v1), variable(v2), boolean(e)."
                + "\nequalityConstraintContainsModifier[v1,v2] = e -> modifier(v1), variable(v2), boolean(e)."
                + "\ninequalityConstraint[v1,v2] = e -> variable(v1), variable(v2), boolean(e)."
                + "\ninequalityConstraintContainsModifier[v1,v2] = e -> modifier(v2), variable(v1), boolean(e)."
                + "\ncomparableConstraint[v1,v2] = e -> variable(v1), variable(v2), boolean(e)."
                + "\nsubtypeConstraint[v1,v2] = s -> variable(v1), variable(v2), boolean(s).\n";
        for (String s : allTypesInString) {
            basicEncoding = basicEncoding + isAnnotated + s
                    + "[v]=i ->variable(v),boolean(i).\n" + mayBeAnnotated + s
                    + "[v]=i ->variable(v),boolean(i).\n" + cannotBeAnnotated
                    + s + "[v]=i ->variable(v),boolean(i).\n"
                    + "AnnotationOf[v] = \"" + s + "\" <-isAnnotated" + s
                    + "[v] = true.\n";
        }
        return basicEncoding;

    }

    public void print() {
        Set<String> Keys = subtype.keySet();
        System.out.println("subtype relation:");
        for (String a : Keys) {
            System.out.println("supertype: " + a + ",subtype: "
                    + subtype.get(a));
        }
        System.out.println("supertype relation");
        for (String b : Keys) {
            System.out.println("subtype: " + b + ",supertype: "
                    + supertype.get(b));
        }
        System.out.println("notComparableFori");
        Set<String> Keys1 = notComparable.keySet();
        for (String c : Keys1) {
            System.out.println("for: " + c + ", not Comparable with: "
                    + notComparable.get(c));
        }

    }
}