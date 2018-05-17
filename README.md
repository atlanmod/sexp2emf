# sexp2emf

Turns [S-expressions](https://en.wikipedia.org/wiki/S-expression) into a [EMF
models](http://www.eclipse.org/modeling/emf/).

This is useful for building models from a readable (i.e., not XMI) text file or
Java string.

## How to use

```java
// A sexp representing an Ecore model
String sexp =
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

// Build the model
EObject[] eObjects = Sexp2EMF.build(sexp, EcoreFactory.eINSTANCE);

// Test against an expected model built using the standard API
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

// They are structurally equal
Assert.assertTrue(EcoreUtil.equals(p, objs[0]));
```

## Redistribution

This program and the accompanying materials are made available under the terms
of the Eclipse Public License 2.0 which is available at
https://www.eclipse.org/legal/epl-2.0/

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License, version 3
which is available at https://www.gnu.org/licenses/gpl-3.0.txt
