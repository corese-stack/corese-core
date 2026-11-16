package fr.inria.corese.core.next.data.api.io.serializer;

import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.api.io.option.RDFSerializationOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.BlankNodeIdGenerationOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.BlankNodeStyle;
import fr.inria.corese.core.next.data.api.io.serializer.option.DatatypePolicyOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.GraphStyleOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.LineEndingOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.LiteralDatatypePolicy;
import fr.inria.corese.core.next.data.api.io.serializer.option.PrefixOrdering;
import fr.inria.corese.core.next.data.api.io.serializer.option.PrettyPrintOptions;

import java.util.Objects;

/**
 * Public options shared by RDF serializers.
 *
 * <p>Each format keeps its own safe defaults for settings not represented here.
 * Format-specific JSON-LD and RDFC configuration remains available through the
 * advanced factory.</p>
 */
public final class RDFSerializerOptions implements
        RDFSerializationOptions,
        BaseIRIOptions,
        LineEndingOptions,
        BlankNodeIdGenerationOptions,
        DatatypePolicyOptions,
        PrettyPrintOptions,
        GraphStyleOptions {

    private final String baseIRI;
    private final String lineEnding;
    private final boolean stableBlankNodeIds;
    private final LiteralDatatypePolicy literalDatatypePolicy;
    private final String indent;
    private final boolean prettyPrint;
    private final int maxLineLength;
    private final boolean sortSubjects;
    private final boolean sortPredicates;
    private final boolean sortObjects;
    private final PrefixOrdering prefixOrdering;
    private final boolean usePrefixes;
    private final boolean useMultilineLiterals;
    private final BlankNodeStyle blankNodeStyle;

    private RDFSerializerOptions(Builder builder) {
        this.baseIRI = builder.baseIRI;
        this.lineEnding = builder.lineEnding;
        this.stableBlankNodeIds = builder.stableBlankNodeIds;
        this.literalDatatypePolicy = builder.literalDatatypePolicy;
        this.indent = builder.indent;
        this.prettyPrint = builder.prettyPrint;
        this.maxLineLength = builder.maxLineLength;
        this.sortSubjects = builder.sortSubjects;
        this.sortPredicates = builder.sortPredicates;
        this.sortObjects = builder.sortObjects;
        this.prefixOrdering = builder.prefixOrdering;
        this.usePrefixes = builder.usePrefixes;
        this.useMultilineLiterals = builder.useMultilineLiterals;
        this.blankNodeStyle = builder.blankNodeStyle;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getBaseIRI() {
        return baseIRI;
    }

    @Override
    public String getLineEnding() {
        return lineEnding;
    }

    @Override
    public boolean stableBlankNodeIds() {
        return stableBlankNodeIds;
    }

    @Override
    public LiteralDatatypePolicy getLiteralDatatypePolicy() {
        return literalDatatypePolicy;
    }

    @Override
    public String getIndent() {
        return indent;
    }

    @Override
    public boolean prettyPrint() {
        return prettyPrint;
    }

    @Override
    public int getMaxLineLength() {
        return maxLineLength;
    }

    @Override
    public boolean sortSubjects() {
        return sortSubjects;
    }

    @Override
    public boolean sortPredicates() {
        return sortPredicates;
    }

    @Override
    public boolean sortObjects() {
        return sortObjects;
    }

    @Override
    public PrefixOrdering getPrefixOrdering() {
        return prefixOrdering;
    }

    @Override
    public boolean usePrefixes() {
        return usePrefixes;
    }

    @Override
    public boolean useMultilineLiterals() {
        return useMultilineLiterals;
    }

    @Override
    public BlankNodeStyle getBlankNodeStyle() {
        return blankNodeStyle;
    }

    public static final class Builder {
        private String baseIRI;
        private String lineEnding = "\n";
        private boolean stableBlankNodeIds;
        private LiteralDatatypePolicy literalDatatypePolicy = LiteralDatatypePolicy.MINIMAL;
        private boolean prettyPrint = true;
        private String indent = "    ";
        private int maxLineLength = 80;
        private boolean sortSubjects;
        private boolean sortPredicates;
        private boolean sortObjects;
        private PrefixOrdering prefixOrdering = PrefixOrdering.ALPHABETICAL;
        private boolean usePrefixes = true;
        private boolean useMultilineLiterals = false;
        private BlankNodeStyle blankNodeStyle = BlankNodeStyle.NAMED;

        private Builder() {
        }

        public Builder baseIRI(String baseIRI) {
            this.baseIRI = baseIRI;
            return this;
        }

        public Builder lineEnding(String lineEnding) {
            this.lineEnding = Objects.requireNonNull(lineEnding, "lineEnding");
            return this;
        }

        public Builder stableBlankNodeIds(boolean stableBlankNodeIds) {
            this.stableBlankNodeIds = stableBlankNodeIds;
            return this;
        }

        public Builder literalDatatypePolicy(LiteralDatatypePolicy literalDatatypePolicy) {
            this.literalDatatypePolicy = Objects.requireNonNull(literalDatatypePolicy, "literalDatatypePolicy");
            return this;
        }

        public Builder prettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        public Builder indent(String indent) {
            this.indent = Objects.requireNonNull(indent, "indent");
            return this;
        }

        public Builder maxLineLength(int maxLineLength) {
            if (maxLineLength < 1) {
                throw new IllegalArgumentException("maxLineLength must be positive");
            }
            this.maxLineLength = maxLineLength;
            return this;
        }

        public Builder sortSubjects(boolean sortSubjects) {
            this.sortSubjects = sortSubjects;
            return this;
        }

        public Builder sortPredicates(boolean sortPredicates) {
            this.sortPredicates = sortPredicates;
            return this;
        }

        public Builder sortObjects(boolean sortObjects) {
            this.sortObjects = sortObjects;
            return this;
        }

        public Builder prefixOrdering(PrefixOrdering prefixOrdering) {
            this.prefixOrdering = Objects.requireNonNull(prefixOrdering, "prefixOrdering");
            return this;
        }

        public Builder usePrefixes(boolean usePrefixes) {
            this.usePrefixes = usePrefixes;
            return this;
        }

        public Builder useMultilineLiterals(boolean useMultilineLiterals) {
            this.useMultilineLiterals = useMultilineLiterals;
            return this;
        }

        public Builder blankNodeStyle(BlankNodeStyle blankNodeStyle) {
            this.blankNodeStyle = Objects.requireNonNull(blankNodeStyle, "blankNodeStyle");
            return this;
        }

        public RDFSerializerOptions build() {
            return new RDFSerializerOptions(this);
        }
    }
}
