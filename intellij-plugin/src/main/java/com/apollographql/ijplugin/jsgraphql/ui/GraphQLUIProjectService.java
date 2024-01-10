/*
The MIT License (MIT)

Copyright (c) 2015-present, Jim Kynde Meyer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


This file was copied from the GraphQL plugin (https://github.com/JetBrains/js-graphql-intellij-plugin/blob/33b4e345cee3e0e00b5c398ea3561179dba53968/src/main/com/intellij/lang/jsgraphql/ui/GraphQLUIProjectService.java)
with these changes:
- Call stripApolloClientDirectives() on the query before sending it to the server
- Use ApolloBundle instead of GraphQLBundle for strings
- Remove unused methods
*/

package com.apollographql.ijplugin.jsgraphql.ui;

import com.apollographql.ijplugin.ApolloBundle;
import com.apollographql.ijplugin.graphql.ApolloClientDirectiveStripper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.intellij.codeInsight.CodeSmellInfo;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.icons.AllIcons;
import com.intellij.json.JsonFileType;
import com.intellij.lang.jsgraphql.ide.config.model.GraphQLConfigEndpoint;
import com.intellij.lang.jsgraphql.ide.config.model.GraphQLConfigSecurity;
import com.intellij.lang.jsgraphql.ide.highlighting.query.GraphQLQueryContext;
import com.intellij.lang.jsgraphql.ide.highlighting.query.GraphQLQueryContextHighlightVisitor;
import com.intellij.lang.jsgraphql.ide.introspection.GraphQLIntrospectionService;
import com.intellij.lang.jsgraphql.ide.introspection.GraphQLIntrospectionUtil;
import com.intellij.lang.jsgraphql.ide.notifications.GraphQLNotificationUtil;
import com.intellij.lang.jsgraphql.ide.project.schemastatus.GraphQLEndpointsModel;
import com.intellij.lang.jsgraphql.ide.project.toolwindow.GraphQLToolWindow;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.CodeSmellDetector;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.components.JBLabel;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.lang.jsgraphql.ui.GraphQLUIProjectService.GRAPH_QL_EDITOR_QUERYING;
import static com.intellij.lang.jsgraphql.ui.GraphQLUIProjectService.GRAPH_QL_ENDPOINTS_MODEL;
import static com.intellij.lang.jsgraphql.ui.GraphQLUIProjectService.GRAPH_QL_VARIABLES_EDITOR;

@SuppressWarnings("UnstableApiUsage")
@Service(Service.Level.PROJECT)
public final class GraphQLUIProjectService {

  private static final Logger LOG = Logger.getInstance(GraphQLUIProjectService.class);

  @NotNull
  private final Project myProject;

  public GraphQLUIProjectService(@NotNull final Project project) {
    myProject = project;
  }

  public void executeGraphQL(@NotNull Editor editor, @NotNull VirtualFile virtualFile) {
    final GraphQLEndpointsModel endpointsModel = editor.getUserData(GRAPH_QL_ENDPOINTS_MODEL);
    if (endpointsModel == null) {
      return;
    }
    final GraphQLConfigEndpoint selectedEndpoint =
        GraphQLIntrospectionUtil.promptForEnvVariables(myProject, endpointsModel.getSelectedItem());
    if (selectedEndpoint == null || selectedEndpoint.getUrl() == null) {
      return;
    }

    final GraphQLQueryContext context = GraphQLQueryContextHighlightVisitor.getQueryContextBufferAndHighlightUnused(editor);
    context.query = ApolloClientDirectiveStripper.stripApolloClientDirectives(editor, context.query);

    Map<String, Object> requestData = new HashMap<>();
    requestData.put("query", context.query);
    try {
      requestData.put("variables", getQueryVariables(editor));
    } catch (JsonSyntaxException jse) {
      Editor errorEditor = editor.getUserData(GRAPH_QL_VARIABLES_EDITOR);
      @NlsSafe String errorMessage = jse.getMessage();
      if (errorEditor != null) {
        errorEditor.getContentComponent().grabFocus();
        final VirtualFile errorFile = FileDocumentManager.getInstance().getFile(errorEditor.getDocument());
        if (errorFile != null) {
          final List<CodeSmellInfo> errors = CodeSmellDetector.getInstance(myProject).findCodeSmells(
              Collections.singletonList(errorFile));
          for (CodeSmellInfo error : errors) {
            errorMessage = error.getDescription();
            errorEditor.getCaretModel().moveToOffset(error.getTextRange().getStartOffset());
            break;
          }
        }
      } else {
        errorEditor = editor;
      }
      final HintManagerImpl hintManager = HintManagerImpl.getInstanceImpl();
      final JComponent label = HintUtil.createErrorLabel(
          ApolloBundle.message("graphql.hint.text.failed.to.parse.variables.as.json", errorMessage));
      final LightweightHint lightweightHint = new LightweightHint(label);
      final Point hintPosition = hintManager.getHintPosition(lightweightHint, errorEditor, HintManager.UNDER);
      hintManager.showEditorHint(lightweightHint, editor, hintPosition, 0, 10000, false, HintManager.UNDER);
      return;
    }
    String requestJson = createQueryJsonSerializer().toJson(requestData);
    final String url = selectedEndpoint.getUrl();
    try {
      final HttpPost request = GraphQLIntrospectionService.createRequest(selectedEndpoint, url, requestJson);
      //noinspection DialogTitleCapitalization
      final Task.Backgroundable task =
          new Task.Backgroundable(myProject, ApolloBundle.message("graphql.progress.title.executing.graphql"), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
              indicator.setIndeterminate(true);
              runQuery(editor, virtualFile, context, url, request, selectedEndpoint);
            }
          };
      ProgressManager.getInstance().run(task);
    } catch (IllegalStateException | IllegalArgumentException e) {
      LOG.warn(e);
      GraphQLNotificationUtil.showGraphQLRequestErrorNotification(myProject, url, e, NotificationType.ERROR, null);
    }
  }

  private void runQuery(@NotNull Editor editor,
      @NotNull VirtualFile virtualFile,
      @NotNull GraphQLQueryContext context,
      @NotNull String url,
      @NotNull HttpPost request,
      @NotNull GraphQLConfigEndpoint endpoint) {
    GraphQLIntrospectionService introspectionService = GraphQLIntrospectionService.getInstance(myProject);
    try {
      GraphQLConfigSecurity sslConfig = GraphQLConfigSecurity.getSecurityConfig(endpoint.getConfig());
      try (final CloseableHttpClient httpClient = introspectionService.createHttpClient(url, sslConfig)) {
        editor.putUserData(GRAPH_QL_EDITOR_QUERYING, true);

        String responseJson;
        Header contentType;
        long start = System.currentTimeMillis();
        long end;
        try (final CloseableHttpResponse response = httpClient.execute(request)) {
          responseJson = StringUtil.notNullize(EntityUtils.toString(response.getEntity()));
          contentType = response.getFirstHeader("Content-Type");
        } finally {
          end = System.currentTimeMillis();
        }

        final boolean reformatJson = contentType != null && contentType.getValue() != null &&
            contentType.getValue().startsWith("application/json");
        final Integer errorCount = getErrorCount(responseJson);
        ApplicationManager.getApplication().invokeLater(() -> {
          TextEditor queryResultEditor = GraphQLToolWindow.getQueryResultEditor(myProject);
          if (queryResultEditor == null) {
            return;
          }

          updateQueryResultEditor(responseJson, queryResultEditor, reformatJson);
          String queryResultText = ApolloBundle.message(
              "graphql.query.result.statistics",
              virtualFile.getName(),
              end - start,
              bytesToDisplayString(responseJson.length())
          );

          if (errorCount != null && errorCount > 0) {
            queryResultText += ApolloBundle.message(
                "graphql.query.result.statistics.error",
                errorCount,
                errorCount > 1
                    ? ApolloBundle.message("graphql.query.result.statistics.multiple.errors")
                    : ApolloBundle.message("graphql.query.result.statistics.single.error")
            );

            if (context.onError != null) {
              context.onError.run();
            }
          }

          GraphQLToolWindow.GraphQLQueryResultHeaderComponent queryResultHeader =
              GraphQLToolWindow.getQueryResultHeader(queryResultEditor);
          if (queryResultHeader == null) return;

          JBLabel queryResultLabel = queryResultHeader.getResultLabel();
          @NlsSafe String resultTextString = queryResultText;
          queryResultLabel.setText(resultTextString);
          queryResultLabel.putClientProperty(GraphQLToolWindow.FILE_URL_PROPERTY, virtualFile.getUrl());
          if (!queryResultLabel.isVisible()) {
            queryResultLabel.setVisible(true);
          }

          JBLabel queryStatusLabel = queryResultHeader.getStatusLabel();
          queryStatusLabel.setVisible(errorCount != null);
          if (queryStatusLabel.isVisible() && errorCount != null) {
            if (errorCount == 0) {
              queryStatusLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 0, 0));
              queryStatusLabel.setIcon(AllIcons.General.InspectionsOK);
            } else {
              queryStatusLabel.setBorder(BorderFactory.createEmptyBorder(2, 12, 0, 4));
              queryStatusLabel.setIcon(AllIcons.Ide.ErrorPoint);
            }
          }

          GraphQLToolWindow.showQueryResultEditor(myProject);
        });
      } finally {
        editor.putUserData(GRAPH_QL_EDITOR_QUERYING, null);
      }
    } catch (IOException | GeneralSecurityException e) {
      LOG.warn(e);
      GraphQLNotificationUtil.showGraphQLRequestErrorNotification(myProject, url, e, NotificationType.WARNING, null);
    }
  }

  private void updateQueryResultEditor(final String responseJson, TextEditor textEditor, boolean reformatJson) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      String documentJson = StringUtil.convertLineSeparators(responseJson);
      if (reformatJson) {
        final PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(myProject);
        final PsiFile jsonPsiFile = psiFileFactory.createFileFromText("", JsonFileType.INSTANCE, documentJson);
        CodeStyleManager.getInstance(myProject).reformat(jsonPsiFile);
        final Document document = jsonPsiFile.getViewProvider().getDocument();
        if (document != null) {
          documentJson = document.getText();
        }
      }

      final Document document = textEditor.getEditor().getDocument();
      document.setText(documentJson);
    });
  }

  @NotNull
  private static Gson createQueryJsonSerializer() {
    return new GsonBuilder()
        .registerTypeAdapter(Double.class, (JsonSerializer<Double>) (number, type, jsonSerializationContext) -> {
          if (!Double.isFinite(number)) {
            throw new IllegalArgumentException(String.format("'%s' is not a valid number", number));
          }

          // convert `12.0` to `12` to conform Int types
          if (number == Math.rint(number)) {
            return new JsonPrimitive(number.intValue());
          }

          return new JsonPrimitive(number);
        })
        // explicit nulls could be a part of a service api
        .serializeNulls()
        .create();
  }

  private static Integer getErrorCount(String responseJson) {
    try {
      final Map<?, ?> res = new Gson().fromJson(responseJson, Map.class);
      if (res != null) {
        final Object errors = res.get("errors");
        if (errors instanceof Collection) {
          return ((Collection<?>) errors).size();
        }
        return 0;
      }
    } catch (JsonSyntaxException ignored) {
    }
    return null;
  }

  private static Object getQueryVariables(Editor editor) {
    final Editor variablesEditor = editor.getUserData(GRAPH_QL_VARIABLES_EDITOR);
    if (variablesEditor != null) {
      final String variables = variablesEditor.getDocument().getText();
      if (!variables.isBlank()) {
        return new Gson().fromJson(variables, Map.class);
      }
    }
    return null;
  }

  private static String bytesToDisplayString(long bytes) {
    if (bytes < 1000) return ApolloBundle.message("graphql.query.result.window.bytes.count", bytes);
    int exp = (int) (Math.log(bytes) / Math.log(1000));
    String pre = ("kMGTPE").charAt(exp - 1) + "";
    return String.format("%.1f %sb", bytes / Math.pow(1000, exp), pre);
  }
}
