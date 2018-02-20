package org.atlanmod.sexp2emf.tests;

import org.atlanmod.sexp2emf.Sexp2EMF;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.Assert;
import org.junit.Test;

public class TestSexp2EMF {

  @Test
  public void sample() {
    String s =
      "(EPackage"
     +" :name 'package'"
     +" :nsURI 'http://package'"
     +" :nsPrefix 'package'"
     +" :eClassifiers ["
     +   "#1(EClass :name 'A'"
     +            " :eStructuralFeatures [(EAttribute :name 'attr' :eType EString)"
     +                                   "(EReference :name 'someB' :eType @2"
     +                                               " :lowerBound 0 :upperBound 1)])"
     +   "#2(EClass :name 'B'"
     +            " :eStructuralFeatures [(EReference :name 'multipleA' :eType @1"
     +                                              " :lowerBound 0 :upperBound -1)])])";

    EObject[] objs = Sexp2EMF.build(s, EcoreFactory.eINSTANCE);

    // Expected model
    EcoreFactory f = EcoreFactory.eINSTANCE;
    EPackage p = f.createEPackage();
    EClass c0 = f.createEClass();
    EClass c1 = f.createEClass();
    EAttribute a0 = f.createEAttribute();
    EReference r0 = f.createEReference();
    EReference r1 = f.createEReference();

    p.setName("package");
    p.setNsURI("http://package");
    p.setNsPrefix("package");
    p.getEClassifiers().add(c0);
    p.getEClassifiers().add(c1);

    c0.setName("A");
    c0.getEStructuralFeatures().add(a0);
    c0.getEStructuralFeatures().add(r0);

    c1.setName("B");
    c1.getEStructuralFeatures().add(r1);

    a0.setName("attr");
    a0.setEType(EcorePackage.Literals.ESTRING);

    r0.setName("someB");
    r0.setEType(c1);
    r0.setUpperBound(1);
    r0.setLowerBound(0);

    r1.setName("multipleA");
    r1.setEType(c0);
    r1.setUpperBound(-1);
    r1.setLowerBound(0);

    // Test
    Assert.assertTrue(EcoreUtil.equals(p, objs[0]));
  }
}
