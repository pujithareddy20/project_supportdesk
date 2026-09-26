import React from 'react';
import { TicketActivity } from '../../types/ticket.types';
import {
  Clock,
  User,
  PlusCircle,
  RefreshCw,
  Tag,
  UserCheck,
  MessageSquare,
  Paperclip,
  Activity,
} from 'lucide-react';

interface TicketActivityTimelineProps {
  activities: TicketActivity[];
}

export const TicketActivityTimeline: React.FC<TicketActivityTimelineProps> = ({ activities }) => {
  const formatDate = (dateStr: string) => {
    try {
      return new Date(dateStr).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'TICKET_CREATED':
        return <PlusCircle className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />;
      case 'STATUS_CHANGED':
        return <RefreshCw className="h-4 w-4 text-blue-600 dark:text-blue-400" />;
      case 'PRIORITY_CHANGED':
        return <Tag className="h-4 w-4 text-amber-600 dark:text-amber-400" />;
      case 'TICKET_ASSIGNED':
        return <UserCheck className="h-4 w-4 text-purple-600 dark:text-purple-400" />;
      case 'COMMENT_ADDED':
        return <MessageSquare className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />;
      case 'ATTACHMENT_UPLOADED':
      case 'ATTACHMENT_REPLACED':
        return <Paperclip className="h-4 w-4 text-teal-600 dark:text-teal-400" />;
      default:
        return <Activity className="h-4 w-4 text-slate-500" />;
    }
  };

  if (activities.length === 0) {
    return (
      <div className="text-xs text-slate-400 italic text-center py-4">
        No recorded activity history yet.
      </div>
    );
  }

  return (
    <div className="relative pl-6 space-y-4 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200 dark:before:bg-slate-800">
      {activities.map((act) => (
        <div key={act.id} className="relative flex flex-col gap-1 text-xs">
          {/* Dot icon container */}
          <div className="absolute -left-6 top-0.5 h-5 w-5 rounded-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 flex items-center justify-center shadow-xs">
            {getIcon(act.activityType)}
          </div>

          <div className="flex items-center justify-between gap-2">
            <span className="font-semibold text-slate-800 dark:text-slate-200">
              {act.description}
            </span>
            <span className="text-[11px] text-slate-400 flex items-center gap-1 shrink-0">
              <Clock className="h-3 w-3" />
              {formatDate(act.createdAt)}
            </span>
          </div>

          {act.performerName && (
            <div className="flex items-center gap-1 text-slate-500 dark:text-slate-400 text-[11px]">
              <User className="h-3 w-3" />
              <span>By: {String(act.performerName)}</span>
            </div>
          )}
        </div>
      ))}
    </div>
  );
};
