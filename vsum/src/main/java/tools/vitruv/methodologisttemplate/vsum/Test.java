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
        // GALETTE SYMBOLIC EXECUTION: Constraints are now automatically collected
        // by the SymbolicComparison class in the reactions file.
        // No manual constraint recording needed here!
        System.out.println("[Test.insertTask] Executing with user choice: " + userInput);

        // Validate input range
        if (userInput < 0 || userInput > 4) {
            System.err.println("Invalid user choice: " + userInput + " (expected 0-4)");
            return;
        }

        // 1) Setup Vitruvius user interaction
        var userInteraction = new TestUserInteraction();
        userInteraction.addNextSingleSelection(userInput);

        // 2) Build and initialize VSUM
        InternalVirtualModel vsum = new VirtualModelBuilder()
                .withStorageFolder(projectDir)
                .withUserInteractorForResultProvider(
                        new TestUserInteraction.ResultProvider(userInteraction))
                .withChangePropagationSpecifications(
                        new Amalthea2ascetChangePropagationSpecification())
                .buildAndInitialize();

        vsum.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);

        // 3) Add component container and task to VSUM
        addComponentContainer(vsum, projectDir);
        addTask(vsum);

        // 4) Merge and save results
        try {
            Path outDir = projectDir.resolve("galette-test-output");
            mergeAndSave(vsum, outDir, "vsum-output.xmi");
        } catch (IOException e) {
            throw new RuntimeException("Could not persist VSUM result", e);
        }
    }

    /**
     * Multi-variable version: Insert TWO tasks with TWO user choices.
     * Used for multi-variable path exploration (5 × 5 = 25 paths).
     *
     * @param projectDir Working directory for VSUM
     * @param userInput1 First user choice (task type for first task)
     * @param userInput2 Second user choice (task type for second task)
     */
    public void insertTwoTasks(Path projectDir, Integer userInput1, Integer userInput2) {
        System.out.println("========================================");
        System.out.println("[DEBUG] insertTwoTasks CALLED");
        System.out.println("[DEBUG] projectDir=" + projectDir);
        System.out.println("[DEBUG] userInput1=" + userInput1);
        System.out.println("[DEBUG] userInput2=" + userInput2);
        System.out.println("========================================");

        // GALETTE SYMBOLIC EXECUTION: Manually collect path constraints for BOTH variables
        try {
            Class<?> pathUtilsClass = Class.forName("edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathUtils");

            // Add domain constraints for BOTH variables
            java.lang.reflect.Method addDomainMethod = pathUtilsClass.getMethod(
                "addIntDomainConstraint", String.class, int.class, int.class);
            addDomainMethod.invoke(null, "user_choice_1", 0, 5);
            addDomainMethod.invoke(null, "user_choice_2", 0, 5);

            System.out.println("[Symbolic] Added domain constraints: 0 <= user_choice_1 < 5, 0 <= user_choice_2 < 5");

            // Add switch constraints for BOTH variables
            java.lang.reflect.Method addSwitchMethod = pathUtilsClass.getMethod(
                "addSwitchConstraint", String.class, int.class);
            addSwitchMethod.invoke(null, "user_choice_1", userInput1);
            addSwitchMethod.invoke(null, "user_choice_2", userInput2);

            System.out.println("[Symbolic] Added switch constraints: user_choice_1 == " + userInput1 + ", user_choice_2 == " + userInput2);

        } catch (Exception e) {
            System.err.println("[Symbolic] Constraint collection failed: " + e.getMessage());
            if (Boolean.getBoolean("DEBUG")) {
                e.printStackTrace();
            }
        }

        // Validate input ranges
        if (userInput1 < 0 || userInput1 > 4 || userInput2 < 0 || userInput2 > 4) {
            System.err.println("Invalid user choices: " + userInput1 + ", " + userInput2 + " (expected 0-4)");
            return;
        }

        // 1) Setup Vitruvius user interaction for BOTH tasks
        var userInteraction = new TestUserInteraction();
        userInteraction.addNextSingleSelection(userInput1); // For first task
        userInteraction.addNextSingleSelection(userInput2); // For second task

        // 2) Build and initialize VSUM
        InternalVirtualModel vsum = new VirtualModelBuilder()
                .withStorageFolder(projectDir)
                .withUserInteractorForResultProvider(
                        new TestUserInteraction.ResultProvider(userInteraction))
                .withChangePropagationSpecifications(
                        new Amalthea2ascetChangePropagationSpecification())
                .buildAndInitialize();

        vsum.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);

        // 3) Add component container
        addComponentContainer(vsum, projectDir);

        // 4) Add FIRST task (triggers first user interaction)
        addTaskWithName(vsum, "task1");

        // 5) Add SECOND task (triggers second user interaction)
        addTaskWithName(vsum, "task2");

        // 6) Merge and save results
        try {
            Path outDir = projectDir.resolve("galette-test-output");
            mergeAndSave(vsum, outDir, "vsum-output.xmi");
        } catch (IOException e) {
            throw new RuntimeException("Could not persist VSUM result", e);
        }
    }

    /* ------------------------------------------------- helpers ------------------------------------------------- */

    private void addComponentContainer(VirtualModel vsum, Path projectDir) {
        System.out.println("[DEBUG] addComponentContainer called with projectDir=" + projectDir);
        System.out.println("[DEBUG] Creating view...");
        CommittableView view = getDefaultView(vsum, List.of(ComponentContainer.class))
                .withChangeDerivingTrait();
        System.out.println("[DEBUG] View created: " + view);

        Path modelPath = projectDir.resolve("example.model");
        URI modelURI = URI.createFileURI(modelPath.toString());
        System.out.println("[DEBUG] Model URI: " + modelURI);
        System.out.println("[DEBUG] Creating ComponentContainer...");
        var container = Model2Factory.eINSTANCE.createComponentContainer();
        System.out.println("[DEBUG] ComponentContainer created: " + container);
        System.out.println("[DEBUG] Calling registerRoot...");

        modifyView(view, v -> {
            System.out.println("[DEBUG] Inside modifyView lambda");
            v.registerRoot(container, modelURI);
            System.out.println("[DEBUG] registerRoot completed");
        });
        System.out.println("[DEBUG] addComponentContainer completed");
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

    private void addTaskWithName(VirtualModel vsum, String taskName) {
        CommittableView view = getDefaultView(vsum, List.of(ComponentContainer.class))
                .withChangeDerivingTrait();
        modifyView(view, v -> {
            var task = Model2Factory.eINSTANCE.createTask();
            task.setName(taskName);
            v.getRootObjects(ComponentContainer.class)
             .iterator()
             .next()
             .getTasks()
             .add(task);
        });
    }
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
