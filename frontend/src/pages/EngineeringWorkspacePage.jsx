import { useState } from "react";
import { Link } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";

const ONBOARDING_DISMISSED_KEY = "iaterap.engineering-onboarding.dismissed";

const workflowSteps = [
    {
        number: "01",
        title: "Set up the engineering context",
        description: "Create or confirm the project, aircraft, subsystem and LRU that the work belongs to.",
        action: "Open LRUs",
        to: "/lrus",
    },
    {
        number: "02",
        title: "Author a test procedure",
        description: "Create a procedure for an LRU, then add ordered test steps with clear pass/fail criteria.",
        action: "Create procedure",
        to: "/procedures/create",
    },
    {
        number: "03",
        title: "Check coverage and close gaps",
        description: "Evaluate procedure coverage, investigate gaps, and improve the evidence before review.",
        action: "View coverage",
        to: "/coverage-dashboard",
    },
    {
        number: "04",
        title: "Submit and review",
        description: "Submit the finished procedure for approval and follow it through the review queue.",
        action: "Open review queue",
        to: "/review-queue",
    },
];

export default function EngineeringWorkspacePage() {
    const [showGuide, setShowGuide] = useState(
        () => window.localStorage.getItem(ONBOARDING_DISMISSED_KEY) !== "true",
    );

    const dismissGuide = () => {
        window.localStorage.setItem(ONBOARDING_DISMISSED_KEY, "true");
        setShowGuide(false);
    };

    const showGuideAgain = () => {
        window.localStorage.removeItem(ONBOARDING_DISMISSED_KEY);
        setShowGuide(true);
    };

    return (
        <ManagementLayout
            title="Engineering Workspace"
            description="Start, track and review aerospace test engineering work from one clear workflow."
        >
            <section className="workspace-hero page-card" aria-labelledby="workspace-start-title">
                <div>
                    <p className="eyebrow">Guided start</p>
                    <h2 id="workspace-start-title" className="section-title">Build evidence with confidence</h2>
                    <p className="helper-text">Follow the workflow from engineering context to approved test procedure. Each step keeps the links and terms you need close at hand.</p>
                </div>
                <div className="workspace-hero__actions">
                    <Link className="button" to="/procedures/create">Create a procedure</Link>
                    <Link className="button button--secondary" to="/projects">Browse projects</Link>
                </div>
            </section>

            {showGuide ? (
                <section className="workspace-guide" aria-labelledby="workflow-title">
                    <div className="section-heading">
                        <div>
                            <p className="eyebrow">First procedure workflow</p>
                            <h2 id="workflow-title" className="section-title">What should I do first?</h2>
                        </div>
                        <button className="button--secondary workspace-guide__dismiss" type="button" onClick={dismissGuide}>Hide guide</button>
                    </div>
                    <ol className="workspace-steps">
                        {workflowSteps.map((step) => (
                            <li className="workspace-step" key={step.number}>
                                <span className="workspace-step__number" aria-hidden="true">{step.number}</span>
                                <div>
                                    <h3>{step.title}</h3>
                                    <p>{step.description}</p>
                                    <Link to={step.to}>{step.action}<span aria-hidden="true"> →</span></Link>
                                </div>
                            </li>
                        ))}
                    </ol>
                    <p className="helper-text workspace-guide__note">The guide does not create or change data. You can reopen it at any time from this workspace.</p>
                </section>
            ) : (
                <section className="page-card workspace-guide-hidden">
                    <p className="helper-text">The first-procedure guide is hidden for this browser.</p>
                    <button className="button--secondary" type="button" onClick={showGuideAgain}>Show guide</button>
                </section>
            )}

            <section className="workspace-quick-actions" aria-labelledby="quick-actions-title">
                <div className="section-heading">
                    <div>
                        <p className="eyebrow">Continue working</p>
                        <h2 id="quick-actions-title" className="section-title">Engineering tools</h2>
                    </div>
                </div>
                <div className="workspace-quick-actions__grid">
                    <Link className="workspace-action-card" to="/procedures">
                        <strong>Test procedures</strong>
                        <span>Open drafts, approved procedures and the builder.</span>
                    </Link>
                    <Link className="workspace-action-card" to="/coverage-dashboard">
                        <strong>Coverage dashboard</strong>
                        <span>See evaluated procedure coverage and gaps by subsystem.</span>
                    </Link>
                    <Link className="workspace-action-card" to="/review-queue">
                        <strong>Review queue</strong>
                        <span>Review or follow procedures awaiting a decision.</span>
                    </Link>
                    <Link className="workspace-action-card" to="/reliability-dashboard">
                        <strong>Reliability analytics</strong>
                        <span>Inspect current synthetic reliability evidence and trends.</span>
                    </Link>
                </div>
            </section>
        </ManagementLayout>
    );
}
