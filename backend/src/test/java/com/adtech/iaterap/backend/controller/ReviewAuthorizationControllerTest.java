package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.security.JwtFilter;
import com.adtech.iaterap.backend.security.SecurityConfig;
import com.adtech.iaterap.backend.service.ProcedureSnapshotService;
import com.adtech.iaterap.backend.service.ReviewerCommentService;
import com.adtech.iaterap.backend.service.TestProcedureService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the reviewer authorization boundary without requiring PostgreSQL. */
@WebMvcTest(controllers = {TestProcedureController.class, ReviewerCommentController.class})
@Import(SecurityConfig.class)
class ReviewAuthorizationControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private TestProcedureService procedures;
    @MockBean private ProcedureSnapshotService snapshots;
    @MockBean private ReviewerCommentService comments;
    @MockBean private JwtFilter jwtFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            ((FilterChain) invocation.getArgument(2)).doFilter(
                    invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    void unauthenticatedReviewerDecisionIsRejected() throws Exception {
        mvc.perform(post("/api/procedures/1/approve").with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(procedures, never()).approve(anyLong());
    }

    @Test
    @WithMockUser(roles = "TEST_ENGINEER")
    void nonReviewerCannotRejectProcedure() throws Exception {
        mvc.perform(post("/api/procedures/1/reject").with(csrf()))
                .andExpect(status().isForbidden());

        verify(procedures, never()).reject(anyLong());
    }

    @Test
    @WithMockUser(roles = "RELIABILITY_ENGINEER")
    void reviewerCanRejectProcedure() throws Exception {
        when(procedures.reject(1L)).thenReturn(null);

        mvc.perform(post("/api/procedures/1/reject").with(csrf()))
                .andExpect(status().isOk());

        verify(procedures).reject(1L);
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCannotCreateReviewerComment() throws Exception {
        mvc.perform(post("/api/reviewer-comments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"procedureHistoryId\":1,\"commentText\":\"Not authorized\"}"))
                .andExpect(status().isForbidden());

        verify(comments, never()).save(any());
    }
}
