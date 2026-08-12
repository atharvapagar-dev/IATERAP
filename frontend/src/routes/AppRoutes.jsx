import { Navigate, Route, Routes } from "react-router-dom";
import { useContext } from "react";

import HealthCheckPage from "../pages/HealthCheckPage";
import LoginPage from "../pages/LoginPage";
import ProjectListPage from "../pages/ProjectListPage";
import ProjectCreatePage from "../pages/ProjectCreatePage";

import AircraftListPage from "../pages/AircraftListPage";
import AircraftCreatePage from "../pages/AircraftCreatePage";

import SubsystemListPage from "../pages/SubsystemListPage";
import SubsystemCreatePage from "../pages/SubsystemCreatePage";

import LruListPage from "../pages/LruListPage";
import LruCreatePage from "../pages/LruCreatePage";
import LruDetailPage from "../pages/LruDetailPage";
import LruEditPage from "../pages/LruEditPage";
import SignalLibraryListPage from "../pages/SignalLibraryListPage";
import SignalLibraryCreatePage from "../pages/SignalLibraryCreatePage";
import SignalLibraryDetailPage from "../pages/SignalLibraryDetailPage";
import SignalLibraryEditPage from "../pages/SignalLibraryEditPage";
import ProtocolTagListPage from "../pages/ProtocolTagListPage";
import ProtocolTagCreatePage from "../pages/ProtocolTagCreatePage";
import ProtocolTagDetailPage from "../pages/ProtocolTagDetailPage";
import ProtocolTagEditPage from "../pages/ProtocolTagEditPage";

import FaultTemplateListPage from "../pages/FaultTemplateListPage";
import FaultTemplateCreatePage from "../pages/FaultTemplateCreatePage";
import FaultTemplateDetailPage from "../pages/FaultTemplateDetailPage";
import FaultTemplateEditPage from "../pages/FaultTemplateEditPage";

import FmeaBranchMappingListPage from "../pages/FmeaBranchMappingListPage";
import FmeaBranchMappingCreatePage from "../pages/FmeaBranchMappingCreatePage";
import FmeaBranchMappingDetailPage from "../pages/FmeaBranchMappingDetailPage";
import FmeaBranchMappingEditPage from "../pages/FmeaBranchMappingEditPage";

import AuditLogListPage from "../pages/AuditLogListPage";

import ProcedureListPage from "../pages/ProcedureListPage";
import ProcedureBuilderPage from "../pages/ProcedureBuilderPage";

import ProtectedRoute from "../components/ProtectedRoute";
import { AuthContext } from "../contexts/authContext";

function HomeRoute() {
    const { token } = useContext(AuthContext);

    return (
        <Navigate
            to={token ? "/projects" : "/login"}
            replace
        />
    );
}

function AppRoutes() {
    return (
        <Routes>
            <Route
                path="/"
                element={<HomeRoute />}
            />

            <Route
                path="/health"
                element={<HealthCheckPage />}
            />

            <Route
                path="/login"
                element={<LoginPage />}
            />

            <Route
                path="/projects"
                element={
                    <ProtectedRoute>
                        <ProjectListPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/projects/create"
                element={
                    <ProtectedRoute>
                        <ProjectCreatePage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/aircraft"
                element={
                    <ProtectedRoute>
                        <AircraftListPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/aircraft/create"
                element={
                    <ProtectedRoute>
                        <AircraftCreatePage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/subsystems"
                element={
                    <ProtectedRoute>
                        <SubsystemListPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/subsystems/create"
                element={
                    <ProtectedRoute>
                        <SubsystemCreatePage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/lrus"
                element={
                    <ProtectedRoute>
                        <LruListPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/lrus/create"
                element={
                    <ProtectedRoute>
                        <LruCreatePage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/lrus/:id"
                element={
                    <ProtectedRoute>
                        <LruDetailPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/lrus/:id/edit"
                element={
                    <ProtectedRoute>
                        <LruEditPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/signal-libraries"
                element={
                    <ProtectedRoute>
                        <SignalLibraryListPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/signal-libraries/create"
                element={
                    <ProtectedRoute>
                        <SignalLibraryCreatePage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/signal-libraries/:id"
                element={
                    <ProtectedRoute>
                        <SignalLibraryDetailPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/signal-libraries/:id/edit"
                element={
                    <ProtectedRoute>
                        <SignalLibraryEditPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/protocol-tags"
                element={
                    <ProtectedRoute>
                        <ProtocolTagListPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/protocol-tags/create"
                element={
                    <ProtectedRoute>
                        <ProtocolTagCreatePage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/protocol-tags/:id"
                element={
                    <ProtectedRoute>
                        <ProtocolTagDetailPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/protocol-tags/:id/edit"
                element={
                    <ProtectedRoute>
                        <ProtocolTagEditPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/fault-templates"
                element={
                    <ProtectedRoute>
                        <FaultTemplateListPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/fault-templates/create"
                element={
                    <ProtectedRoute>
                        <FaultTemplateCreatePage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/fault-templates/:id"
                element={
                    <ProtectedRoute>
                        <FaultTemplateDetailPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/fault-templates/:id/edit"
                element={
                    <ProtectedRoute>
                        <FaultTemplateEditPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/fmea-branch-mappings"
                element={
                    <ProtectedRoute>
                        <FmeaBranchMappingListPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/fmea-branch-mappings/create"
                element={
                    <ProtectedRoute>
                        <FmeaBranchMappingCreatePage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/fmea-branch-mappings/:id"
                element={
                    <ProtectedRoute>
                        <FmeaBranchMappingDetailPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/fmea-branch-mappings/:id/edit"
                element={
                    <ProtectedRoute>
                        <FmeaBranchMappingEditPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/audit-logs"
                element={
                    <ProtectedRoute>
                        <AuditLogListPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/procedures"
                element={
                    <ProtectedRoute>
                        <ProcedureListPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/procedures/new"
                element={
                    <ProtectedRoute>
                        <ProcedureBuilderPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/procedures/:id"
                element={
                    <ProtectedRoute>
                        <ProcedureBuilderPage />
                    </ProtectedRoute>
                }
            />
        </Routes>
    );
}

export default AppRoutes;
