package org.atlanmod.sexp2emf.tests;

import static org.hamcrest.CoreMatchers.is;

import java.util.Collections;

import org.atlanmod.sexp2emf.Sexp2EMF;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.junit.Assert;
import org.junit.Test;

import fr.inria.atlanmod.emfviews.virtuallinks.ConcreteConcept;
import fr.inria.atlanmod.emfviews.virtuallinks.ContributingModel;
import fr.inria.atlanmod.emfviews.virtuallinks.VirtualConcept;
import fr.inria.atlanmod.emfviews.virtuallinks.VirtualLinksFactory;
import fr.inria.atlanmod.emfviews.virtuallinks.WeavingModel;

public class TestSexp2EMF {

  @Test
  public void sample() {
    // TODO: rewrite example using Ecore to drop dependency on VirtualLinks?

    String p = "(WeavingModel"
        +      " :name 'extension1'"
        +      " :virtualLinks [(VirtualConcept :name 'X' :superConcepts [@1])]"
        +      " :contributingModels [(ContributingModel"
        +                             " :URI 'http://www.eclipse.org/uml2/5.0/UML'"
        +                             " :concreteElements [#1(ConcreteConcept :path 'Class')])])";

    EObject[] objs = Sexp2EMF.build(p, VirtualLinksFactory.eINSTANCE);

    // Expected model
    VirtualLinksFactory f = VirtualLinksFactory.eINSTANCE;
    WeavingModel wm0 = f.createWeavingModel();
    VirtualConcept vc0 = f.createVirtualConcept();
    ContributingModel cm0 = f.createContributingModel();
    ConcreteConcept cc0 = f.createConcreteConcept();

    wm0.setName("extension1");
    wm0.getVirtualLinks().add(vc0);
    wm0.getContributingModels().add(cm0);

    vc0.setName("X");
    vc0.getSuperConcepts().add(cc0);

    cm0.setURI("http://www.eclipse.org/uml2/5.0/UML");
    cm0.getConcreteElements().add(cc0);

    cc0.setPath("Class");

    // Test
    DefaultComparisonScope scope = new DefaultComparisonScope(wm0, objs[0], wm0);
    Comparison comp = EMFCompare.builder().build().compare(scope);
    EList<Diff> diffs = comp.getDifferences();
    Assert.assertThat(diffs, is(Collections.EMPTY_LIST));
  }
}
