package org.atlanmod.sexp2emf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.atlanmod.sexp2emf.SexpParser.Atom;
import org.atlanmod.sexp2emf.SexpParser.BoolLiteral;
import org.atlanmod.sexp2emf.SexpParser.Call;
import org.atlanmod.sexp2emf.SexpParser.IntLiteral;
import org.atlanmod.sexp2emf.SexpParser.Node;
import org.atlanmod.sexp2emf.SexpParser.Ref;
import org.atlanmod.sexp2emf.SexpParser.Sexp;
import org.atlanmod.sexp2emf.SexpParser.StringLiteral;
import org.atlanmod.sexp2emf.SexpParser.Target;
import org.atlanmod.sexp2emf.SexpParser.Visitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EcorePackageImpl;

public class EMFBuilder implements Visitor<Void> {

  List<Call> calls = new ArrayList<>();
  Map<Sexp, Integer> targets = new HashMap<>();

  @Override
  public Void onNode(Node n) {
    for (Sexp s : n.children) {
      s.accept(this);
    }
    return null;
  }

  @Override
  public Void onCall(Call c) {
    calls.add(c);
    for (Sexp s : c.children) {
      s.accept(this);
    }
    return null;
  }

  @Override
  public Void onTarget(Target t) {
    targets.put(t.sexp, t.id);
    t.sexp.accept(this);
    return null;
  }

  @Override
  public Void onRef(Ref r) {
    return null;
  }

  @Override
  public Void onAtom(Atom a) {
    return null;
  }

  @Override
  public Void onString(StringLiteral s) {
    return null;
  }

  @Override
  public Void onInt(IntLiteral i) {
    return null;
  }

  @Override
  public Void onBool(BoolLiteral b) {
    return null;
  }

  public EObject[] build(EFactory factory) {
    Map<Call, EObject> objs = new HashMap<>();
    Map<Integer, EObject> runtimeTargets = new HashMap<>();

    // First create all classes, to allow for cycles
    for (Call c : calls) {
      String className = atomValue(c.children[0]);
      String methodName = String.format("create%s", className);
      EObject o;

      try {
        // @Refactor maybe we can use some EMF provided reflection?
        o = (EObject) factory.getClass().getMethod(methodName).invoke(factory);
      } catch (Exception e) {
        throw new CompileException("Cannot find constructor for class '%s' on factory %s",
                                   className);
      }

      // If this is targeted call, register the created object to the map
      if (targets.containsKey(c)) {
        runtimeTargets.put(targets.get(c), o);
      }

      objs.put(c, o);
    }

    // Now populate objects with properties
    for (Map.Entry<Call, EObject> kv : objs.entrySet()) {
      Call c = kv.getKey();
      EObject o = kv.getValue();

      // Skip first child (the class name), and step each (:property, value) pair
      for (int i = 1; i < c.children.length; i += 2) {

        // This should be a `:feature-name`
        String featureName = atomValue(c.children[i]).substring(1);

        EStructuralFeature feature = o.eClass().getEStructuralFeature(featureName);
        if (feature == null) {
          throw new CompileException("No feature named '%s' in '%s'", featureName,
                                     o.eClass().getName());
        }

        // The value can be many things
        Object val = value(c.children[i + 1], runtimeTargets, objs);

        // If the value is a list, we have to first get the feature and add
        // each value to it.
        if (val instanceof Object[]) {
          Object list = o.eGet(feature);
          if (!(list instanceof EList)) {
            throw new CompileException("");
          }

          EList<Object> l = ((EList<Object>) list);
          for (Object v : (Object[]) val) {
            l.add(v);
          }
        } else {
          o.eSet(feature, val);
        }
      }
    }

    // The output array has the EObject in the same order as the Calls were visited.
    // Most important is that the root call is the first in the output array.
    EObject[] ret = new EObject[calls.size()];
    for (int i = 0; i < ret.length; ++i) {
      ret[i] = objs.get(calls.get(i));
    }
    return ret;
  }

  static private String atomValue(Sexp s) {
    if (s instanceof Atom) {
      return ((Atom) s).value;
    } else {
      throw new CompileException("Expected an atom, got '%s'", s);
    }
  }

  static private Object value(Sexp s, Map<Integer, EObject> targets, Map<Call, EObject> callsToObjs) {
    // @Refactor: if this was a visitor, it could statically fail on new added AST node types
    if (s instanceof Atom) {
      // Atoms refer to built-in Ecore classifiers.  Maybe there is a more generic syntax we could
      // use for datatypes or Ecore built-ins?
      String name = ((Atom) s).value;
      EClassifier c = EcorePackage.eINSTANCE.getEClassifier(name);
      if (c == null) {
        throw new CompileException("Unknown classifier '%s'", name);
      }
      return c;
    } else if (s instanceof BoolLiteral) {
      return ((BoolLiteral) s).value;
    } else if (s instanceof StringLiteral) {
      return ((StringLiteral) s).value;
    } else if (s instanceof IntLiteral) {
      return ((IntLiteral) s).value;
    } else if (s instanceof Ref) {
      return targets.get(((Ref) s).id);
    } else if (s instanceof Call) {
      return callsToObjs.get(s);
    } else if (s instanceof Node) {
      Node n = (Node) s;
      Object[] v = new Object[n.children.length];
      for (int i = 0; i < n.children.length; ++i) {
        v[i] = value(n.children[i], targets, callsToObjs);
      }
      return v;
    } else if (s instanceof Target) {
      return value(((Target) s).sexp, targets, callsToObjs);
    } else {
      throw new CompileException("Cannot value '%s'", s);
    }
  }

  static class CompileException extends RuntimeException {
    private static final long serialVersionUID = -3574849511229453442L;

    public CompileException(String msg, Object... args) {
      super(String.format(msg, args));
    }
  }
}
