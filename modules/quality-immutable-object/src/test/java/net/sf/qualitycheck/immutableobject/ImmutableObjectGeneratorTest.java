package net.sf.qualitycheck.immutableobject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.sf.qualitycheck.immutableobject.domain.ImmutableSettings;
import net.sf.qualitycheck.immutableobject.generator.ImmutableObjectGenerator;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;

public class ImmutableObjectGeneratorTest {

	private static final Logger LOG = LoggerFactory.getLogger(ImmutableObjectGeneratorTest.class);

	private ImmutableSettings.Builder settingsBuilder;

	@Before
	public void before() {
		settingsBuilder = new ImmutableSettings.Builder();

		// global settings
		settingsBuilder.fieldPrefix("");
		settingsBuilder.jsr305Annotations(false);
		settingsBuilder.guava(true);
		settingsBuilder.qualityCheck(false);

		// immutable settings
		settingsBuilder.serializable(false);

		// builder settings
		settingsBuilder.builderCopyConstructor(false);
		settingsBuilder.builderFlatMutators(false);
		settingsBuilder.builderFluentMutators(false);
		settingsBuilder.builderName("");
		settingsBuilder.builderImplementsInterface(false);

	}

	private String readInterfaceAndGenerate(final String name, final ImmutableSettings settings) throws IOException {
		final InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
		return ImmutableObjectGenerator.generate(CharStreams.toString(new InputStreamReader(stream)), settings);
	}

	private String readReferenceImmutable(final String name) throws IOException {
		final InputStream stream = getClass().getClassLoader().getResourceAsStream("Immutable" + name);
		return CharStreams.toString(new InputStreamReader(stream));
	}

	@Test
	public void renderingOf_fieldPrefix() {
		final StringBuilder b = new StringBuilder();
		b.append("interface TestObject {\n");
		b.append("String getName();\n");
		b.append("}");
		final ImmutableSettings settings = settingsBuilder.fieldPrefix("_").build();
		final String generatedCode = ImmutableObjectGenerator.generate(b.toString(), settings);
		assertTrue(generatedCode.contains("private final String _name;"));
	}

	@Test
	public void renderingOf_guava() {
		final StringBuilder b = new StringBuilder();
		b.append("import java.util.List;\n");
		b.append("interface TestObject {\n");
		b.append("List<String> getNames();\n");
		b.append("}");

		final ImmutableSettings settingsWithGuava = settingsBuilder.guava(true).build();
		final String generatedCodeWithGuava = ImmutableObjectGenerator.generate(b.toString(), settingsWithGuava);
		assertTrue(generatedCodeWithGuava.contains("this.names = ImmutableList.copyOf(names);"));
		assertTrue(generatedCodeWithGuava.contains("return Objects.equal(this.names, other.names);"));
		assertTrue(generatedCodeWithGuava.contains("return Objects.hashCode(this.names);"));

		final ImmutableSettings settingsWithoutGuava = settingsBuilder.guava(false).build();
		final String generatedCodeWithoutGuava = ImmutableObjectGenerator.generate(b.toString(), settingsWithoutGuava);
		assertTrue(generatedCodeWithoutGuava.contains("this.names = Collections.unmodifiableList(new ArrayList<String>(names));"));
		assertTrue(generatedCodeWithoutGuava.contains("} else if (!names.equals(other.names))"));
		assertTrue(generatedCodeWithoutGuava.contains("result = prime * result + (names == null ? 0 : names.hashCode());"));
	}

	@Test
	public void renderingOf_jsr305Annotations() {
		final StringBuilder b = new StringBuilder();
		b.append("import javax.annotation.Nonnull;");
		b.append("interface TestObject {\n");
		b.append("@Nonnull String getName();\n");
		b.append("}");
		final ImmutableSettings settings = settingsBuilder.jsr305Annotations(true).build();
		final String generatedCode = ImmutableObjectGenerator.generate(b.toString(), settings);
		assertTrue(generatedCode.contains("@Immutable\npublic final class"));
		assertTrue(generatedCode.contains("@Nonnull\n\tpublic static ImmutableTestObject copyOf(@Nonnull "));
		assertTrue(generatedCode.contains("public ImmutableTestObject(@Nonnull "));
		assertTrue(generatedCode.contains("@Nonnull\n\tprivate final String name;"));
		assertTrue(generatedCode.contains("@Nonnull\n\tpublic String getName() {"));
	}

	@Test
	public void renderingOf_qualityCheck() {
		final StringBuilder b = new StringBuilder();
		b.append("import javax.annotation.Nonnull;");
		b.append("interface TestObject {\n");
		b.append("@Nonnull String getName();\n");
		b.append("}");
		final ImmutableSettings settings = settingsBuilder.qualityCheck(true).build();
		final String generatedCode = ImmutableObjectGenerator.generate(b.toString(), settings);
		assertTrue(generatedCode.contains("Check.notNull(testobject, \"testobject\");"));
		assertTrue(generatedCode.contains("this.name = Check.notNull(name, \"name\");"));
	}

	@Test
	public void renderingOf_serializable() throws IOException {
		final StringBuilder b = new StringBuilder();
		b.append("interface TestObject {\n");
		b.append("String getName();\n");
		b.append("}");
		final ImmutableSettings settings = settingsBuilder.serializable(true).build();
		final String generatedCode = ImmutableObjectGenerator.generate(b.toString(), settings);
		assertTrue(generatedCode.contains("class ImmutableTestObject implements TestObject, Serializable {"));
		assertTrue(generatedCode.contains("private static final long serialVersionUID = 1L;"));
	}

	@Test
	public void testCarInterface() throws IOException {
		final ImmutableSettings.Builder settings = new ImmutableSettings.Builder();

		// global settings
		settings.fieldPrefix("");
		settings.jsr305Annotations(true);
		settings.guava(false);
		settings.qualityCheck(true);

		// immutable settings
		settings.serializable(false);

		// builder settings
		settings.builderCopyConstructor(true);
		settings.builderFlatMutators(false);
		settings.builderFluentMutators(true);
		settings.builderName("Builder");
		settings.builderImplementsInterface(true);

		final String file = "Car.java";
		assertEquals(readReferenceImmutable(file), readInterfaceAndGenerate(file, settings.build()));
	}

	@Test
	public void testSettingsInterface() throws IOException {
		final ImmutableSettings.Builder settings = new ImmutableSettings.Builder();

		// global settings
		settings.fieldPrefix("");
		settings.jsr305Annotations(true);
		settings.guava(true);
		settings.qualityCheck(true);

		// immutable settings
		settings.serializable(false);

		// builder settings
		settings.builderCopyConstructor(true);
		settings.builderFlatMutators(true);
		settings.builderFluentMutators(true);
		settings.builderName("Builder");
		settings.builderImplementsInterface(false);

		final String file = "Settings.java";
		LOG.info("\n" + readInterfaceAndGenerate(file, settings.build()));
	}

}
