import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import TestStepCard from "../components/TestStepCard";
import { testProcedureService } from "../services/testProcedureService";
import { reviewerCommentService } from "../services/reviewerCommentService";

import "../assets/styles/ReviewQueue.css";

/**
 * ReviewQueuePage (AI-191 / T8-B2)
 * 
 * Displays procedures awaiting review (approval_status = IN_REVIEW).
 * Supports two views:
 * 1. List view: table of procedures in review queue
 * 2. Detail view: inspect procedure, test steps, and add reviewer comments
 */
export default function ReviewQueuePage() {
    const navigate = useNavigate();
    const { procedureId } = useParams();

    // List view state
    const [procedures, setProcedures] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    // Detail view state
    const [selectedProcedure, setSelectedProcedure] = useState(null);
    const [selectedRevisionId, setSelectedRevisionId] = useState(null);
    const [reviewerComments, setReviewerComments] = useState([]);
    const [commentText, setCommentText] = useState("");
    const [submittingComment, setSubmittingComment] = useState(false);
    const [commentError, setCommentError] = useState("");
    const [detailLoading, setDetailLoading] = useState(false);

    const loadComments = async (procedure) => {
        try {
            setDetailLoading(true);
            const historyResponse = await testProcedureService.getHistory(procedure.id);
            const revisions = historyResponse.data || [];
            const latest = revisions[revisions.length - 1];
            if (!latest) {
                setSelectedRevisionId(null);
                setReviewerComments([]);
                return;
            }
            setSelectedRevisionId(latest.id);
            const commentsResponse = await reviewerCommentService.getByRevision(latest.id);
            setReviewerComments(commentsResponse.data || []);
        } catch (err) {
            console.error("Failed to load comments:", err);
        } finally {
            setDetailLoading(false);
        }
    };

    // Load procedures on mount
    useEffect(() => {
        const loadQueue = async () => {
            try {
                setError("");
                const response = await testProcedureService.getReviewQueue();
                setProcedures(response.data);

                // If a procedureId is in params, load detail view
                if (procedureId) {
                    const procedure = response.data.find((p) => p.id === Number(procedureId));
                    if (procedure) {
                        setSelectedProcedure(procedure);
                        await loadComments(procedure);
                    }
                }
            } catch (fetchError) {
                console.error("Failed to fetch review queue:", fetchError);
                setError("Unable to load the review queue right now.");
            } finally {
                setLoading(false);
            }
        };

        void loadQueue();
    }, [procedureId]);

    const handleSelectProcedure = (procedure) => {
        setSelectedProcedure(procedure);
        navigate(`/procedures/review-queue/${procedure.id}`);
        void loadComments(procedure);
    };

    const handleCloseDetail = () => {
        setSelectedProcedure(null);
        setSelectedRevisionId(null);
        setCommentText("");
        setCommentError("");
        navigate("/procedures/review-queue");
    };

    const handleAddComment = async () => {
        if (!commentText.trim()) {
            setCommentError("Comment cannot be empty.");
            return;
        }

        try {
            setSubmittingComment(true);
            setCommentError("");
            if (!selectedRevisionId) {
                throw new Error("No procedure revision is available for this comment.");
            }
            await reviewerCommentService.createComment({
                procedureHistoryId: selectedRevisionId,
                commentText: commentText.trim(),
            });
            await loadComments(selectedProcedure);
            setCommentText("");
        } catch (err) {
            console.error("Failed to add comment:", err);
            setCommentError("Unable to add comment right now.");
        } finally {
            setSubmittingComment(false);
        }
    };

    // List view
    if (!selectedProcedure) {
        return (
            <ManagementLayout
                title="Review Queue"
                description="Procedures awaiting review and approval"
            >
                <div className="page-toolbar">
                    <p className="helper-text">
                        Select a procedure to review its details, test steps, and add comments.
                    </p>
                </div>

                <section className="page-card stack">
                    {error ? <p style={{ color: "var(--danger)" }}>{error}</p> : null}

                    {loading ? (
                        <p className="helper-text">Loading review queue...</p>
                    ) : !procedures.length ? (
                        <div className="empty-state">
                            No procedures are currently awaiting review.
                        </div>
                    ) : (
                        <table className="entity-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Name</th>
                                    <th>LRU</th>
                                    <th>Version</th>
                                    <th>Status</th>
                                    <th>Steps</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {procedures.map((item) => (
                                    <tr key={item.id}>
                                        <td>{item.id}</td>
                                        <td><strong>{item.name}</strong></td>
                                        <td>{item.lru?.name || "-"}</td>
                                        <td>{item.versionNumber}</td>
                                        <td>{item.approvalStatus}</td>
                                        <td>{item.steps?.length || 0}</td>
                                        <td>
                                            <div className="table-actions">
                                                <button 
                                                    onClick={() => handleSelectProcedure(item)}
                                                >
                                                    Review
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </section>
            </ManagementLayout>
        );
    }

    // Detail view
    return (
        <ManagementLayout
            title={`Review: ${selectedProcedure.name}`}
            description={selectedProcedure.description || "No description"}
        >
            <div className="review-detail-container">
                <button 
                    className="btn-secondary"
                    onClick={handleCloseDetail}
                    style={{ marginBottom: "1.5rem" }}
                >
                    ← Back to Review Queue
                </button>

                <section className="page-card stack">
                    <h3>Procedure Information</h3>
                    <div className="info-grid">
                        <div>
                            <label>Procedure ID</label>
                            <p>{selectedProcedure.id}</p>
                        </div>
                        <div>
                            <label>Name</label>
                            <p>{selectedProcedure.name}</p>
                        </div>
                        <div>
                            <label>LRU</label>
                            <p>{selectedProcedure.lru?.name || "-"}</p>
                        </div>
                        <div>
                            <label>Version</label>
                            <p>{selectedProcedure.versionNumber}</p>
                        </div>
                        <div>
                            <label>Status</label>
                            <p style={{ color: "var(--info)" }}>
                                {selectedProcedure.approvalStatus}
                            </p>
                        </div>
                        <div>
                            <label>Total Steps</label>
                            <p>{selectedProcedure.steps?.length || 0}</p>
                        </div>
                    </div>
                    {selectedProcedure.description && (
                        <div style={{ marginTop: "1rem" }}>
                            <label>Description</label>
                            <p>{selectedProcedure.description}</p>
                        </div>
                    )}
                </section>

                <section className="page-card stack">
                    <h3>Test Steps</h3>
                    {detailLoading ? (
                        <p className="helper-text">Loading test steps...</p>
                    ) : !selectedProcedure.steps || !selectedProcedure.steps.length ? (
                        <div className="empty-state">
                            This procedure has no test steps.
                        </div>
                    ) : (
                        <div className="steps-list">
                            {selectedProcedure.steps.map((step, index) => (
                                <TestStepCard
                                    key={step.id}
                                    step={step}
                                    lruId={selectedProcedure.lruId}
                                    readOnly={true}
                                    isFirst={index === 0}
                                    isLast={index === selectedProcedure.steps.length - 1}
                                />
                            ))}
                        </div>
                    )}
                </section>

                <section className="page-card stack">
                    <h3>Reviewer Comments</h3>
                    <div className="reviewer-comments-section">
                        <div className="comments-list">
                            {reviewerComments.length === 0 ? (
                                <p className="helper-text">
                                    No comments yet. Add one below.
                                </p>
                            ) : (
                                reviewerComments.map((comment) => (
                                    <div key={comment.id} className="comment-item">
                                        <strong>{comment.author || "Reviewer"}</strong>
                                        <small>{new Date(comment.createdAt).toLocaleString()}</small>
                                        <p>{comment.commentText}</p>
                                    </div>
                                ))
                            )}
                        </div>

                        <div className="comment-form">
                            <label htmlFor="comment-text">Add Comment</label>
                            <textarea
                                id="comment-text"
                                value={commentText}
                                onChange={(e) => setCommentText(e.target.value)}
                                placeholder="Enter your review feedback here..."
                                rows={4}
                                disabled={submittingComment}
                            />
                            {commentError && (
                                <p style={{ color: "var(--danger)", fontSize: "0.9rem" }}>
                                    {commentError}
                                </p>
                            )}
                            <button
                                onClick={handleAddComment}
                                disabled={submittingComment || !commentText.trim()}
                            >
                                {submittingComment ? "Submitting..." : "Add Comment"}
                            </button>
                        </div>
                    </div>
                </section>
            </div>
        </ManagementLayout>
    );
}
