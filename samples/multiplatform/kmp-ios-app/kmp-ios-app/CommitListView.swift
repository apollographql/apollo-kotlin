//
//  CommitListView.swift
//  kmp-ios-app
//
//  Created by Ellen Shapiro on 4/26/20.
//

import kmp_lib_sample
import SwiftUI

struct CommitListView: View {
    @ObservedObject var repoManager: RepositoryManager
    let repo: RepositoryFragment
    
    var body: some View {
        List(repoManager.commits[repo.name] ?? []) { commit in
            CommitCell(commit: commit)
        }.navigationBarTitle("Commits")
    }
}

struct CommitListView_Previews: PreviewProvider {
    static var previews: some View {
        let repo = RepositoryFragmentImpl.Data(__typename: "__typename",
                                      id: "1",
                                      name: "TestRepo",
                                      repoDescription: "a test repo")
        let manager = RepositoryManager()
        return NavigationView {
            CommitListView(repoManager: manager, repo: repo)
        }
    }
}

extension GithubRepositoryCommitsQueryDataViewerRepositoryRefTargetCommitTarget.HistoryEdgesNode: Identifiable {}
