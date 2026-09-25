const LABELS = {
    "COVERAGE-INTERFACE": "Interface",
    "COVERAGE-SIGNAL": "Signal",
    "COVERAGE-REQUIREMENT": "Requirement",
    "COVERAGE-FUNCTIONAL": "Functional",
    "COVERAGE-FAULT": "Fault",
    "COVERAGE-FMEA": "FMEA",
};

function formatScore(value) {
    return value == null ? "—" : `${Number(value).toFixed(1)}%`;
}

export default function CoverageSummaryCard({ coverage, loading, error, onRefresh }) {
    return (
        <section className="page-card coverage-summary">
            <div className="coverage-summary__header">
                <div>
                    <p className="eyebrow">Coverage engine</p>
                    <h2 className="section-title">Procedure coverage</h2>
                </div>
                <button type="button" className="button--secondary" onClick={onRefresh} disabled={loading}>
                    {loading ? "Evaluating…" : "Evaluate"}
                </button>
            </div>

            {error ? <p className="coverage-summary__error">{error}</p> : null}
            {!coverage && !loading ? (
                <p className="helper-text">Evaluate this saved revision to see category coverage and the composite score.</p>
            ) : null}
            {coverage ? (
                <>
                    <div className="coverage-summary__score">
                        <span className="coverage-summary__score-label">Composite score</span>
                        <strong>{formatScore(coverage.compositeScore)}</strong>
                        <span className="helper-text">Revision {coverage.versionNumber}</span>
                    </div>
                    <div className="coverage-summary__grid">
                        {Object.entries(coverage.categories || {}).map(([ruleId, value]) => {
                            const threshold = coverage.thresholdResults?.[ruleId];
                            return (
                                <div className="coverage-summary__metric" key={ruleId}>
                                    <div className="coverage-summary__metric-title">
                                        <span>{LABELS[ruleId] || ruleId}</span>
                                        <span className={threshold === false ? "coverage-summary__threshold is-failing" : "coverage-summary__threshold"}>
                                            {threshold == null ? "Unavailable" : threshold ? "Pass" : "Review"}
                                        </span>
                                    </div>
                                    <strong>{formatScore(value)}</strong>
                                    <small>Weight {formatScore((coverage.weights?.[ruleId] || 0) * 100)}</small>
                                </div>
                            );
                        })}
                    </div>
                    {coverage.gaps?.length ? (
                        <div className="coverage-gaps">
                            <h3 className="section-title">Structured gaps</h3>
                            {coverage.gaps.map((gap) => <div className="coverage-gap" key={`${gap.elementType}-${gap.elementId}`}><strong>{gap.elementType} #{gap.elementId}</strong><span>{gap.description}</span></div>)}
                        </div>
                    ) : <p className="helper-text">No uncovered defined elements were detected.</p>}
                </>
            ) : null}
        </section>
    );
}
