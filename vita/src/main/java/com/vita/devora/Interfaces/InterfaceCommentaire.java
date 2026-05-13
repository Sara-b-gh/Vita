package com.vita.devora.Interfaces;

import com.vita.devora.Entities.CommentDisplay;

import java.sql.SQLException;
import java.util.List;

public interface InterfaceCommentaire<C> {
    void AddComment(C comment) throws SQLException;
    void Updatecomment(C comment) throws SQLException;
    void DeleteComment(int id) throws SQLException;
    List<C> GetAllComments() throws SQLException;
    C GetCommentById(int id) throws SQLException;
    List<C> getByPost(int postId);
    List<C> getReplies(int parentId);
    List<C> getAllSortedByLatest();

    List<C> getAllSortedByOldest();

    List<CommentDisplay> getAllCommentDisplays();
}
