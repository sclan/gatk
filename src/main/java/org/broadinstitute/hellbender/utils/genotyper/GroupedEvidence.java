package org.broadinstitute.hellbender.utils.genotyper;

import htsjdk.samtools.util.Locatable;
import org.broadinstitute.hellbender.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class GroupedEvidence<EVIDENCE extends Locatable>  extends ArrayList<EVIDENCE> implements Locatable {
    private final String contig;
    private final int start;
    private final int end;

    public GroupedEvidence(final List<EVIDENCE> evidence) {
        super(evidence);
        Utils.nonEmpty(evidence, "Must have at least one unit of evidence");
        contig = evidence.get(0).getContig();
        start = evidence.stream().mapToInt(Locatable::getStart).min().getAsInt();
        end = evidence.stream().mapToInt(Locatable::getStart).max().getAsInt();
    }

    @Override
    public String getContig() { return contig; }

    @Override
    public int getStart() { return start; }

    @Override
    public int getEnd() { return end; }

}
