import { Routes, Route } from "react-router-dom";

import HealthCheckPage from "../pages/HealthCheckPage";
import LruListPage from "../pages/LruListPage";
import LruCreatePage from "../pages/LruCreatePage";
import LruDetailPage from "../pages/LruDetailPage";
import LruEditPage from "../pages/LruEditPage";

function AppRoutes() {
  return (
    <Routes>
      <Route path="/health" element={<HealthCheckPage />} />

      <Route path="/lrus" element={<LruListPage />} />
      <Route path="/lrus/create" element={<LruCreatePage />} />
      <Route path="/lrus/:id" element={<LruDetailPage />} />
      <Route path="/lrus/:id/edit" element={<LruEditPage />} />
    </Routes>
  );
}

export default AppRoutes;
