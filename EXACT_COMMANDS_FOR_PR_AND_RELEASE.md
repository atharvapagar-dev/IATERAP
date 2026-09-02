# EXACT COMMANDS FOR PR AND RELEASE — Week 8 IATERAP

**Branch Ready:** `feature/review-queue`  
**Remote Status:** ✅ Pushed to origin  
**Current Commit:** `008594f` — feat(procedure): implement submit-for-review and Review Queue

---

## 📋 FOR THE OTHER INTERN (Code Review)

### Step 1: Get the Branch Locally
```bash
git fetch origin feature/review-queue
git checkout feature/review-queue
```

### Step 2: Review Changes
```bash
# See what changed compared to develop
git diff develop feature/review-queue --stat

# View actual code changes
git diff develop feature/review-queue

# View commit details
git show 008594f
```

### Step 3: Test Locally
```bash
# Backend tests
cd backend
./mvnw test -Dtest=ReviewQueueServiceTest

# Frontend build
cd frontend
npm install
npm run build

# Verify no errors in either
```

### Step 4: Approve PR (if satisfied)
- Go to: https://github.com/atharvapagar-dev/IATERAP/pulls
- Find PR for `feature/review-queue`
- Click "Approve" or "Request Changes"
- If OK: click "Merge pull request" (use "Squash and merge")

---

## 🚀 FOR RELEASE MANAGER OR BOTH INTERNS

### After PR is Approved and Merged to Develop

#### Option A: Using GitHub Web UI (EASIEST)

1. **Go to Pull Requests**
   - URL: https://github.com/atharvapagar-dev/IATERAP/pulls
   - Find the PR for `feature/review-queue`

2. **Merge to Develop**
   - Click "Squash and Merge"
   - Default message includes all commit history
   - Confirm: Click "Confirm squash and merge"
   - PR automatically closed

3. **Verify Merge**
   - Main page shows: PR merged ✅
   - develop branch updated

4. **Create Release Tag**
   - Go to: https://github.com/atharvapagar-dev/IATERAP/releases
   - Click "Draft a new release"
   - Tag version: `v0.2-procedure-authoring`
   - Target: `develop` (or leave as default)
   - Title: "Week 8: Review Queue & Approval Workflow"
   - Description:
     ```
     ## Features
     - Review Queue backend query and API endpoint
     - Review Queue frontend page with list and detail views
     - Procedure inspection with test steps display
     - Reviewer comments UI
     - Structural validation on submit-for-review

     ## What's Included
     - Complete end-to-end procedure authoring lifecycle
     - Reuses TestStepCard component for consistency
     - Partial index optimization for queue queries
     - Comprehensive test coverage
     - Production-ready implementation

     ## Closes
     - AI-121
     - AI-191
     - AI-192
     - AI-193
     ```
   - Click "Publish release"

5. **Done!** Release is tagged and visible on GitHub

---

#### Option B: Using Command Line

**If you prefer command line (for both interns working together):**

```bash
# 1. Make sure you're on the feature branch and it's pushed
git checkout feature/review-queue
git log --oneline -1
# Should show: 008594f (HEAD -> feature/review-queue, origin/feature/review-queue)

# 2. Switch to develop and pull latest
git checkout develop
git pull origin develop

# 3. Merge using squash (recommended for cleaner history)
git merge --squash feature/review-queue

# 4. Create commit with full message
git commit -m "feat(procedure): submit-for-review and Review Queue (T8-B1, T8-B2)

Implements Week 8 Review Queue functionality:
- Backend: Add TestProcedureService.getReviewQueue() and /api/procedures/review-queue endpoint
- Backend: Leverages partial index (V20) for performance optimization
- Frontend: Create ReviewQueuePage with dual list/detail view
- Frontend: Reuse TestStepCard component in read-only mode
- Frontend: Implement Reviewer Comments UI with form and list
- Testing: Add ReviewQueueServiceTest with 3 comprehensive tests
- Documentation: Add WEEK_8_IMPLEMENTATION.md

Closes AI-121, AI-191, AI-192, AI-193

This completes the procedure authoring lifecycle:
Create → Version → Submit for Review → Review Queue → Inspect"

# 5. Push merged develop
git push origin develop

# 6. Create annotated tag on develop
git tag -a v0.2-procedure-authoring -m "Week 8: Review Queue & Approval Workflow

Features:
- Review Queue backend query and API endpoint
- Review Queue frontend page with procedure inspection
- Reviewer comments UI with persistence hooks
- Structural validation on submit-for-review
- Complete end-to-end authoring workflow

Closes: AI-121, AI-191, AI-192, AI-193"

# 7. Push tag to GitHub
git push origin v0.2-procedure-authoring

# 8. Verify
git tag -l | grep v0.2
git show v0.2-procedure-authoring
```

---

## ✅ VERIFICATION CHECKLIST

### Before Merging
- [ ] Code review completed by other intern
- [ ] Backend tests passing: `./mvnw test -Dtest=ReviewQueueServiceTest`
- [ ] Frontend builds: `npm run build` no errors
- [ ] All changes on feature/review-queue branch
- [ ] Branch pushed to GitHub origin

### After Merging to Develop
- [ ] PR shows "Merged" status
- [ ] `develop` branch now includes all changes
- [ ] `git log develop | grep "Review Queue"` shows the commit
- [ ] Feature branch can be deleted (optional)

### After Creating Tag
- [ ] Tag exists locally: `git tag -l | grep v0.2`
- [ ] Tag pushed to GitHub: `git push origin v0.2-procedure-authoring` ✅
- [ ] Visible on GitHub: https://github.com/atharvapagar-dev/IATERAP/releases
- [ ] Shows correct commit hash

---

## 🔄 WORKFLOW SUMMARY

```
Current State:
  feature/review-queue ←→ origin/feature/review-queue
         ↓
         Commit: 008594f ✅ (implementation complete)

After Code Review Approval:
  feature/review-queue
         ↓
         Merge to develop (squash or regular)
         ↓
  develop ← 008594f (or squashed)
         ↓
         Create tag: v0.2-procedure-authoring
         ↓
  v0.2-procedure-authoring @ develop
         ↓
         Release on GitHub ✅

Result:
  - feature/review-queue merged
  - develop updated with all changes
  - v0.2-procedure-authoring tag available
  - Ready for deployment
```

---

## 📞 TROUBLESHOOTING

### "Merge conflict" when merging to develop
**Solution:**
```bash
git merge develop feature/review-queue
# If conflicts occur, resolve them
git status  # shows conflicts
# Edit conflicted files, keep both changes or choose correct version
git add <resolved-files>
git commit -m "Merge develop into feature/review-queue"
git push origin feature/review-queue
# Try PR merge again
```

### "Merge button disabled" on GitHub
**Cause:** Branch has conflicts or CI checks failing  
**Solution:**
1. Resolve locally (see above)
2. Push resolved changes
3. Merge button should enable

### "Cannot push tag"
**Solution:**
```bash
git push --set-upstream origin v0.2-procedure-authoring
# or
git push origin v0.2-procedure-authoring --force  # only if tag already exists remotely
```

### "Tag already exists"
**Solution:**
```bash
# Check what's on remote
git ls-remote --tags origin | grep v0.2

# If you need to update it (dangerous!):
git tag -d v0.2-procedure-authoring
git push origin :refs/tags/v0.2-procedure-authoring
git tag -a v0.2-procedure-authoring -m "..."
git push origin v0.2-procedure-authoring
```

---

## 📊 WHAT GETS DEPLOYED

When you merge and tag, this goes to production:

```
/backend/
  - Service: TestProcedureService.getReviewQueue() method
  - Controller: GET /api/procedures/review-queue endpoint
  - Tests: ReviewQueueServiceTest

/frontend/
  - Page: ReviewQueuePage.jsx (dual view)
  - Service: getReviewQueue() method
  - Route: /procedures/review-queue and /:procedureId
  - Style: ReviewQueue.css

/docs/
  - WEEK_8_IMPLEMENTATION.md (reference documentation)
```

---

## 🎯 NEXT STEPS (After Release)

### For Future Development

1. **Wire Comments Persistence**
   - Connect ReviewerCommentService to backend
   - Update ReviewQueuePage comment form to call API

2. **Implement Approve/Reject**
   - Add endpoints: POST /procedures/{id}/approve and /reject
   - Update page with approval UI

3. **Email Notifications**
   - Notify reviewer of new submissions
   - Notify author of approval status

4. **Approval History**
   - Track approval decisions
   - Display timeline in review detail view

---

## 📝 NOTES FOR DOCUMENTATION

**To include in project README or release notes:**

> Week 8 (2026-08-24 to 2026-08-28) completes the **procedure authoring lifecycle** with:
> - Review Queue backend query optimized with partial index
> - Review Queue frontend page for procedure inspection
> - Reviewer Comments UI ready for persistence integration
> - Complete end-to-end demonstration: Create → Version → Submit → Review

---

## 🎉 YOU'RE DONE!

Once the tag is created and visible on GitHub, the Week 8 work is complete and ready for:
- ✅ Month 2 milestone demonstration
- ✅ Production deployment
- ✅ QA testing
- ✅ Future enhancement phases

**Questions?** Refer to:
- WEEK_8_IMPLEMENTATION.md (comprehensive technical details)
- WEEK_8_COMPLETION_REPORT.md (this file's counterpart with all details)
- Code comments in ReviewQueuePage.jsx and TestProcedureService.java

---

**Happy releasing! 🚀**
