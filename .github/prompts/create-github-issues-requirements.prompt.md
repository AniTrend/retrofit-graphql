---
mode: 'agent'
description: 'Create GitHub Issues for feature request from specification, unmet specification and implementation requirements'
tools: ['codebase', 'search', 'github', 'create_issue', 'search_issues', 'update_issue']
---
Create GitHub Issues for unimplemented requirements in the specification at `${file}`.

## Process

1. Analyze specification file to extract all requirements
2. Check codebase implementation status for each requirement
3. Search existing issues using `search_issues` to avoid duplicates
4. Create new issue per unimplemented requirement using `create_issue`
5. Use issue templates https://github.com/AniTrend/anitrend-v2/blob/develop/.github/ISSUE_TEMPLATE (fallback to `.github/ISSUE_TEMPLATE/feature-request.md`)

## Requirements

- One issue per unimplemented requirement from specification
- Clear requirement ID and description mapping
- Include implementation guidance and acceptance criteria
- Verify against existing issues before creation

## Issue Content

- Title: [module:sub_module] Brief description of what the requiment is (if no sumodule then `[module]` will suffice)
- Description: Detailed requirement, implementation method, and context following the `ISSUE_TEMPLATE` applicable
- Labels: `feature request`, `enhancement request` or `bug` (as appropriate, use `get_labels` if such exists to see what labels exist for the repo)
- Type: `Feature`, `Task`, `Bug` are the main issue types that currently exist and should be used

## Implementation Check

- Search codebase for related code patterns
- Check related specification files in `/spec/` directory
- Verify requirement isn't partially implemented
