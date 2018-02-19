package org.atlanmod.sexp2emf;

import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;

public final class Sexp2EMF {

  private Sexp2EMF() {}

  public static EObject[] build(String sexp, EFactory f) {
    EMFBuilder b = new EMFBuilder();
    SexpParser.parse(sexp).accept(b);
    return b.build(f);
  }

}
