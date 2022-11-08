Tests that deprecation and opt-in requirements on enum values and input fields do not trigger a warning when compiling. 

For regular fields, the solution is to stop using these fields but for enum values and input fields, the user doesn't control that because they will be included in codegen in all cases.