package org.broadinstitute.hellbender.tools.walkers.annotator;

import com.google.common.annotations.VisibleForTesting;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFormatHeaderLine;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.genotyper.AlleleLikelihoods;
import org.broadinstitute.hellbender.utils.help.HelpConstants;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.variant.GATKVCFConstants;
import org.broadinstitute.hellbender.utils.variant.GATKVCFHeaderLines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Number of forward and reverse reads that support REF and ALT alleles
 *
 * <p>Strand bias is a type of sequencing bias in which one DNA strand is favored over the other, which can result in incorrect evaluation of the amount of evidence observed for one allele vs. the other. The StrandBiasBySample annotation produces read counts per allele and per strand that are used by other annotation modules (FisherStrand and StrandOddsRatio) to estimate strand bias using statistical approaches.
 *
 * <p>This annotation produces 4 values, corresponding to the number of reads that support the following (in that order):</p>
 * <ul>
 *     <li>the reference allele on the forward strand</li>
 *     <li>the reference allele on the reverse strand</li>
 *     <li>the alternate allele on the forward strand</li>
 *     <li>the alternate allele on the reverse strand</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>GT:AD:GQ:PL:SB  0/1:53,51:99:1758,0,1835:23,30,33,18</pre>
 * <p>In this example, the reference allele is supported by 23 forward reads and 30 reverse reads, the alternate allele is supported by 33 forward reads and 18 reverse reads.</p>
 *
 * <h3>Caveats</h3>
 * <ul>
 *     <li>This annotation can only be generated by HaplotypeCaller (it will not work when called from VariantAnnotator).</li>
 * </ul>
 *
 * <h3>Related annotations</h3>
 * <ul>
 *     <li><b><a href="https://www.broadinstitute.org/gatk/guide/tooldocs/org_broadinstitute_gatk_tools_walkers_annotator_FisherStrand.php">FisherStrand</a></b> uses Fisher's Exact Test to evaluate strand bias.</li>
 *     <li><b><a href="https://www.broadinstitute.org/gatk/guide/tooldocs/org_broadinstitute_gatk_tools_walkers_annotator_StrandOddsRatio.php">StrandOddsRatio</a></b> is an updated form of FisherStrand that uses a symmetric odds ratio calculation.</li>
 * </ul>
 */
@DocumentedFeature(groupName=HelpConstants.DOC_CAT_ANNOTATORS, groupSummary=HelpConstants.DOC_CAT_ANNOTATORS_SUMMARY, summary="Number of forward and reverse reads that support REF and ALT alleles (SB)")
public final class StrandBiasBySample extends GenotypeAnnotation implements StandardMutectAnnotation {
    private final static Logger logger = LogManager.getLogger(StrandBiasBySample.class);

    @Override
    public void annotate(final ReferenceContext ref,
                         final VariantContext vc,
                         final Genotype g,
                         final GenotypeBuilder gb,
                         final AlleleLikelihoods<GATKRead, Allele> likelihoods) {
        Utils.nonNull(vc);
        Utils.nonNull(g);
        Utils.nonNull(gb);

        if ( likelihoods == null || !g.isCalled() ) {
            logger.warn("Annotation will not be calculated, genotype is not called or alleleLikelihoodMap is null");
            return;
        }

        final int[][] table = FisherStrand.getContingencyTable(likelihoods, vc, 0, Arrays.asList(g.getSampleName()));

        gb.attribute(GATKVCFConstants.STRAND_BIAS_BY_SAMPLE_KEY, getContingencyArray(table));
    }

    //For now this is only for 2x2 contingency tables
    private static final int ARRAY_DIM = 2;

    /**
     * Helper function to turn the FisherStrand 2x2 table into the SB annotation array
     * @param table the 2x2 table used by the FisherStrand annotation
     * @return the array used by the per-sample Strand Bias annotation
     */
    @VisibleForTesting
    static List<Integer> getContingencyArray(final int[][] table) {
        if(table.length != ARRAY_DIM || table[0].length != ARRAY_DIM) {
            throw new IllegalArgumentException("Expecting a " + ARRAY_DIM + "x" + ARRAY_DIM + " strand bias table.");
        }

        final List<Integer> list = new ArrayList<>(ARRAY_DIM * ARRAY_DIM);
        list.add(table[0][0]);
        list.add(table[0][1]);
        list.add(table[1][0]);
        list.add(table[1][1]);
        return list;
    }

    @Override
    public List<String> getKeyNames() {
        return Collections.singletonList(GATKVCFConstants.STRAND_BIAS_BY_SAMPLE_KEY);
    }

    @Override
    public List<VCFFormatHeaderLine> getDescriptions() {
        return Collections.singletonList(GATKVCFHeaderLines.getFormatLine(getKeyNames().get(0)));
    }

    public static int getAltForwardCountFromFlattenedContingencyTable(final int[] contingencyTable) {
        return contingencyTable[ARRAY_DIM];
    }
    public static int getAltReverseCountFromFlattenedContingencyTable(final int[] contingencyTable) {
        return contingencyTable[ARRAY_DIM+1];
    }
}
