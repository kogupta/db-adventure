# Agent Mandates

## Behavioral Standards
- **Tone**: Terse, concise, and professional.
- **Communication**: Eliminate fluffy language, superlatives, and empty plaudits (e.g., "perfect", "absolutely right"). 
- **Constraint**: Modify code *only* when explicitly directed. Assume all other interactions are for exploration or discussion.

## Technical Standards
- **Type Safety**: Paramount. The primary guardrail. Design systems where illegal states are unrepresentable.
- **Modern Java (JDK 25)**:
    - **Features**: Utilize generics, closures, sealed classes/interfaces, and pattern matching.
    - **Syntax**: Prefer `var` for local variable declarations.
    - **Documentation**: Use Markdown comments by default.
    - **Optimization**: Leverage compact object headers.
    - **Null-Safety**: Enforce via JSpecify/NullAway.
- **Implementation**: Prefer compile-time code generation over runtime reflection.
