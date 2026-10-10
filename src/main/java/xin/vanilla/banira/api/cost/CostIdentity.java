package xin.vanilla.banira.api.cost;

import java.util.Objects;
import java.util.UUID;

public final class CostIdentity {
    private final String domain, businessSignature;
    private final String configurationSignature;
    private final UUID payer;
    private final long operationId, generation;

    public CostIdentity(String domain, UUID payer, long operationId, long generation, String businessSignature) {
        this(domain, payer, operationId, generation, businessSignature, "");
    }

    public CostIdentity(String domain, UUID payer, long operationId, long generation, String businessSignature, String configurationSignature) {
        this.domain = Objects.requireNonNull(domain, "domain");
        this.payer = Objects.requireNonNull(payer, "payer");
        this.businessSignature = Objects.requireNonNull(businessSignature, "businessSignature");
        this.configurationSignature = Objects.requireNonNull(configurationSignature, "configurationSignature");
        if (domain.isEmpty() || operationId <= 0 || generation < 0) throw new IllegalArgumentException("Invalid cost identity");
        this.operationId = operationId;
        this.generation = generation;
    }

    public String domain() { return domain; }
    public UUID payer() { return payer; }
    public long operationId() { return operationId; }
    public long generation() { return generation; }
    public String businessSignature() { return businessSignature; }
    public String configurationSignature() { return configurationSignature; }
}
