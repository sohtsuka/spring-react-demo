package com.example.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.apache.ibatis.annotations.Mapper;

import org.springframework.stereotype.Service;

/**
 * ArchUnit によるアーキテクチャー適合性テスト。
 *
 * <p>このプロジェクトは以下のレイヤー構造を採用している:
 *
 * <pre>
 *   Controller → Service → Repository → DB
 * </pre>
 *
 * <p>例外: security パッケージは Spring Security の UserDetailsService 実装のため Repository
 * を直接参照する（認証コンテキスト確立前に呼ばれるため Service 層を経由できない）。この例外は設計上の意図としてルール定義に明示している。
 */
@AnalyzeClasses(packagesOf = Application.class, importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureTest {

    // -----------------------------------------------------------------------
    // Rule 1: レイヤーアーキテクチャーの依存方向を強制
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule LAYERED_ARCHITECTURE = layeredArchitecture().consideringAllDependencies().layer("Controller")
            .definedBy("com.example.app.controller..").layer("Service").definedBy("com.example.app.service..")
            .layer("Repository").definedBy("com.example.app.repository..").layer("Model")
            .definedBy("com.example.app.model..").layer("Config").definedBy("com.example.app.config..")
            .layer("Security").definedBy("com.example.app.security..").layer("Exception")
            .definedBy("com.example.app.exception..").layer("Util").definedBy("com.example.app.util..")
            .whereLayer("Controller").mayOnlyBeAccessedByLayers("Config").whereLayer("Service")
            .mayOnlyBeAccessedByLayers("Controller", "Security", "Config")
            // Security は Spring Security の制約上 Repository を直接参照する（設計上の例外）
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Security", "Config").whereLayer("Model")
            .mayOnlyBeAccessedByLayers("Controller", "Service", "Repository", "Security", "Exception", "Util",
                    "Config");

    // -----------------------------------------------------------------------
    // Rule 2: Controller は Repository を直接呼んではならない
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule CONTROLLERS_MUST_NOT_ACCESS_REPOSITORIES = noClasses().that()
            .resideInAPackage("com.example.app.controller..").should().dependOnClassesThat()
            .resideInAPackage("com.example.app.repository..")
            .as("Controller は Repository を直接呼んではならない — Service 層を経由すること");

    // -----------------------------------------------------------------------
    // Rule 3: Controller は Entity を直接参照してはならない（DTO 経由のみ許可）
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule ENTITIES_MUST_NOT_LEAK_INTO_CONTROLLERS = noClasses().that()
            .resideInAPackage("com.example.app.controller..").should().dependOnClassesThat()
            .resideInAPackage("com.example.app.model.entity..").as("Controller は Entity を直接参照してはならない — DTO を使用すること");

    // -----------------------------------------------------------------------
    // Rule 4: repository パッケージのクラスは @Mapper インターフェースのみ許可
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule REPOSITORIES_MUST_BE_MAPPER_INTERFACES = classes().that()
            .resideInAPackage("com.example.app.repository..").should().beInterfaces().andShould()
            .beAnnotatedWith(Mapper.class).as("repository パッケージには @Mapper アノテーションを持つインターフェースのみ配置すること");

    // -----------------------------------------------------------------------
    // Rule 5: UserService の実装は service.impl パッケージに配置する
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule SERVICE_IMPLS_MUST_BE_IN_IMPL_PACKAGE = classes().that()
            .implement(com.example.app.service.UserService.class).should()
            .resideInAPackage("com.example.app.service.impl..").as("UserService の実装は service.impl パッケージに配置すること");

    // -----------------------------------------------------------------------
    // Rule 6: service.impl パッケージのクラスは @Service アノテーションを持つこと
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule SERVICE_IMPLS_MUST_BE_ANNOTATED_WITH_SERVICE = classes().that()
            .resideInAPackage("com.example.app.service.impl..").should().beAnnotatedWith(Service.class)
            .as("service.impl パッケージのクラスには @Service アノテーションが必要");

    // -----------------------------------------------------------------------
    // Rule 7: パッケージ間に循環依存があってはならない
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule NO_CYCLIC_DEPENDENCIES = slices().matching("com.example.app.(*)..").should().beFreeOfCycles()
            .as("パッケージ間に循環依存があってはならない");
}
