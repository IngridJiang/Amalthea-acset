package tools.vitruv.methodologisttemplate.vsum;

import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;
import tools.vitruv.methodologisttemplate.model.model2.Model2Factory;
import tools.vitruv.methodologisttemplate.model.model2.ComponentContainer;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;

import mir.reactions.amalthea2ascet.Amalthea2ascetChangePropagationSpecification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;

public class Test {

    public void insertTask(Path projectDir, int userInput) {
        // 1) 
        var userInteraction = new TestUserInteraction();
        userInteraction.addNextSingleSelection(userInput);

        // 2)  VSUM
        InternalVirtualModel vsum = new VirtualModelBuilder()
                .withStorageFolder(projectDir)
                .withUserInteractorForResultProvider(
                        new TestUserInteraction.ResultProvider(userInteraction))
                .withChangePropagationSpecifications(
                        new Amalthea2ascetChangePropagationSpecification())
                .buildAndInitialize();

        vsum.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);

        // 3) VSUM
        addComponentContainer(vsum, projectDir);
        addTask(vsum);

        // 4) merge and save
        try {
            Path outDir = projectDir.resolve("galette-test-output");
            mergeAndSave(vsum, outDir, "vsum-output.xmi");
        } catch (IOException e) {
            throw new RuntimeException("Could not persist VSUM result", e);
        }
    }

    /* ------------------------------------------------- helpers ------------------------------------------------- */

    private void addComponentContainer(VirtualModel vsum, Path projectDir) {
        CommittableView view = getDefaultView(vsum, List.of(ComponentContainer.class))
                .withChangeDerivingTrait();
        modifyView(view, v -> v.registerRoot(
                Model2Factory.eINSTANCE.createComponentContainer(),
                URI.createFileURI(projectDir.resolve("example.model").toString())));
    }

    private void addTask(VirtualModel vsum) {
        CommittableView view = getDefaultView(vsum, List.of(ComponentContainer.class))
                .withChangeDerivingTrait();
        modifyView(view, v -> {
            var task = Model2Factory.eINSTANCE.createTask();
            task.setName("specialname");
            v.getRootObjects(ComponentContainer.class)
             .iterator()
             .next()
             .getTasks()
             .add(task);
        });
    }

    /** */
    private View getDefaultView(VirtualModel vsum, Collection<Class<?>> rootTypes) {
        var selector = vsum.createSelector(
                ViewTypeFactory.createIdentityMappingViewType("default"));
        selector.getSelectableElements().stream()
                .filter(e -> rootTypes.stream().anyMatch(t -> t.isInstance(e)))
                .forEach(e -> selector.setSelected(e, true));
        return selector.createView();
    }

    /** */
    private void modifyView(CommittableView view, Consumer<CommittableView> change) {
        change.accept(view);
        view.commitChanges();
    }

    /** */
    private static void mergeAndSave(InternalVirtualModel vm,
                                     Path outDir,
                                     String fileName) throws IOException {
        Files.createDirectories(outDir);

        ResourceSet rs = new ResourceSetImpl();
        URI mergedUri = URI.createFileURI(outDir.resolve(fileName).toString());
        Resource merged = rs.createResource(mergedUri);

        for (Resource src : vm.getViewSourceModels()) {
            for (EObject obj : src.getContents()) {
                merged.getContents().add(EcoreUtil.copy(obj));
            }
        }

        Map<String, Object> opts = Map.of(
                XMLResource.OPTION_ENCODING, "UTF-8",
                XMLResource.OPTION_FORMATTED, Boolean.TRUE,
                XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE
        );
        merged.save(opts);
    }
}
