//
//  CommitCell.swift
//  kmp-ios-app
//
//  Created by Ellen Shapiro on 4/27/20.
//

import SwiftUI
import kmp_lib_sample

struct CommitCell: View {
    let commit: GithubRepositoryCommitsQueryDataViewerRepositoryRefTargetCommitTarget.HistoryEdgesNode
    
    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                Text(commit.author?.name ?? "(Unknown Author)")
                    .italic()
                    .bold()
                Spacer()
                Text(commit.abbreviatedOid)
                    .italic()
            }
            Text(commit.message)
        }
    }
}

struct CommitCell_Previews: PreviewProvider {
    static var previews: some View {
        let author = GithubRepositoryCommitsQueryDataViewerRepositoryRefTargetCommitTarget.HistoryEdgesNodeAuthor(name: "Test Author",
                                                         email: "not@real.com")
        let node = GithubRepositoryCommitsQueryDataViewerRepositoryRefTargetCommitTarget.HistoryEdgesNode(
                                                     id: "1",
                                                     messageHeadline: "A short commit message",
                                                     abbreviatedOid: "fcaafb",
                                                     message: "A short commmit message\n\nA considerably longer explanation of that message that could take up a lot of space",
                                                     author: author)
        return CommitCell(commit: node)
    }
}
