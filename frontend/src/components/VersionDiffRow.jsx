const CHANGE_TYPE_META = {
    ADDED: { label: "Added" },
    REMOVED: { label: "Removed" },
    CHANGED: { label: "Changed" },
    UNCHANGED: { label: "Unchanged" },
};

function displayValue(value) {
    return value === null || value === undefined || value === "" ? "—" : value;
}

/**
 * AI-183 / T7-B2: reusable Version Diff Row.
 *
 * Renders a single field-level difference produced by the Version
 * Comparison engine (AI-182 / T7-B1). Purely presentational: it takes the
 * field name, old/new values, and a change state as props and never fetches
 * or hard-codes any procedure data itself, so it can be reused anywhere a
 * single field comparison needs to be displayed.
 *
 * @param {string} fieldName - name of the compared field (e.g. "expectedBehaviour")
 * @param {string|number|null} [oldValue] - value on the "from" revision
 * @param {string|number|null} [newValue] - value on the "to" revision
 * @param {"ADDED"|"REMOVED"|"CHANGED"|"UNCHANGED"} [changeType] - state to render; defaults to UNCHANGED
 */
export default function VersionDiffRow({ fieldName, oldValue, newValue, changeType }) {
    const normalizedType = (changeType || "UNCHANGED").toUpperCase();
    const meta = CHANGE_TYPE_META[normalizedType] || CHANGE_TYPE_META.UNCHANGED;

    return (
        <div className={`diff-row diff-row--${normalizedType.toLowerCase()}`}>
            <div className="diff-row__field">
                <span className="diff-row__field-name">{fieldName}</span>
                <span className="status-pill diff-row__badge">{meta.label}</span>
            </div>

            {normalizedType === "UNCHANGED" && (
                <p className="diff-row__value">{displayValue(newValue ?? oldValue)}</p>
            )}

            {normalizedType === "ADDED" && (
                <p className="diff-row__value diff-row__value--new">{displayValue(newValue)}</p>
            )}

            {normalizedType === "REMOVED" && (
                <p className="diff-row__value diff-row__value--old">{displayValue(oldValue)}</p>
            )}

            {normalizedType === "CHANGED" && (
                <div className="diff-row__pair">
                    <p className="diff-row__value diff-row__value--old">
                        <span className="diff-row__value-label">OLD</span>
                        {displayValue(oldValue)}
                    </p>
                    <p className="diff-row__value diff-row__value--new">
                        <span className="diff-row__value-label">NEW</span>
                        {displayValue(newValue)}
                    </p>
                </div>
            )}
        </div>
    );
}
